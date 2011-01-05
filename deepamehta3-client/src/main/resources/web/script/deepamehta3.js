var dm3c = new function() {

    // Settings
    var CORE_SERVICE_URI = "/core"
    this.SEARCH_FIELD_WIDTH = 16    // in chars
    var UPLOAD_DIALOG_WIDTH = "50em"
    var GENERIC_TOPIC_ICON_SRC = "images/gray-dot.png"

    var EXCLUDE_TYPES_FROM_MENUS = [
        "de/deepamehta/core/topictype/Workspace",
        "de/deepamehta/core/topictype/Topicmap",
        "de/deepamehta/core/topictype/Icon",
        "de/deepamehta/core/topictype/SearchResult",
        "de/deepamehta/core/topictype/TopicmapRelationRef",
        "de/deepamehta/core/topictype/Plugin"
    ]

    var ENABLE_LOGGING = false
    var LOG_PLUGIN_LOADING = false
    var LOG_IMAGE_LOADING = false
    this.LOG_GUI = false

    this.restc = new RESTClient(CORE_SERVICE_URI)
    this.type_cache = new TypeCache()
    this.ui = new UIHelper()
    this.render = new RenderHelper()

    this.selected_topic = null  // topic being displayed, or null if no one is currently displayed (a Topic object)
    this.current_rel_id = null  // ID of relation being activated, or null if no one is currently activated
    this.canvas = null          // the canvas that displays the topicmap (a Canvas object)
    //
    var plugin_sources = []
    var plugins = {}            // key: plugin class, value: plugin instance
    var doctype_impl_sources = []
    var doctype_impls = {}
    var field_renderer_sources = []
    var css_stylesheets = []

    // log window
    if (ENABLE_LOGGING) {
        var log_window = window.open()
    }

    // ------------------------------------------------------------------------------------------------------ Public API



    /**************/
    /*** Topics ***/
    /**************/



    /**
     * Creates a topic in the DB.
     *
     * High-level utility method for plugin developers.
     *
     * @param   type_uri        The topic type URI, e.g. "de/deepamehta/core/topictype/Note".
     * @param   properties      Optional: topic properties (object, key: field URI, value: content).
     *
     * @return  The topic as stored in the DB.
     */
    this.create_topic = function(type_uri, properties) {
        var topic = {
            type_uri: type_uri,
            properties: properties || {}
        }
        topic = dm3c.restc.create_topic(topic)
        // trigger hook
        dm3c.trigger_hook("post_create_topic", topic)
        //
        return topic
    }

    /**
     * Updates topic properties in the DB and and memory.
     * Triggers the "post_update_topic" hook.
     *
     * High-level utility method for plugin developers.
     */
    this.update_topic = function(topic, properties) {
        // update DB
        dm3c.restc.set_topic_properties(topic.id, properties)
        // update memory
        var old_properties = {}
        js.copy(topic.properties, old_properties)
        js.copy(properties, topic.properties)
        // trigger hook
        dm3c.trigger_hook("post_update_topic", topic, old_properties)
    }

    /**
     * Deletes a topic (including its relations) from the DB and the GUI, and triggers the "post_delete_topic" hook.
     *
     * High-level utility method for plugin developers.
     */
    this.delete_topic = function(topic) {
        // update DB
        dm3c.restc.delete_topic(topic.id)
        // trigger hook
        dm3c.trigger_hook("post_delete_topic", topic)
        // update GUI
        dm3c.hide_topic(topic.id, true)      // is_part_of_delete_operation=true
    }

    /**
     * Hides a topic (including its relations) from the GUI (canvas & detail panel).
     *
     * High-level utility method for plugin developers.
     */
    this.hide_topic = function(topic_id, is_part_of_delete_operation) {
        // canvas
        dm3c.canvas.remove_all_relations_of_topic(topic_id, is_part_of_delete_operation)
        dm3c.canvas.remove_topic(topic_id, true, is_part_of_delete_operation)    // refresh=true
        // detail panel
        if (topic_id == dm3c.selected_topic.id) {
            dm3c.selected_topic = null
            dm3c.render_topic()
        } else {
            alert("WARNING: removed topic which was not selected\n" +
                "(removed=" + topic_id + " selected=" + dm3c.selected_topic.id + ")")
        }
    }



    /*****************/
    /*** Relations ***/
    /*****************/



    /**
     * Creates a relation in the DB.
     *
     * High-level utility method for plugin developers.
     *
     * @param   type_id             The relation type ID, e.g. "RELATION", "SEARCH_RESULT".
     * @param   properties          Optional: relation properties (object, key: field URI, value: content).
     *
     * @return  The relation as stored in the DB.
     */
    this.create_relation = function(type_id, src_topic_id, dst_topic_id, properties) {
        var relation = {
            type_id: type_id,
            src_topic_id: src_topic_id,
            dst_topic_id: dst_topic_id,
            properties: properties || {}
        }
        // FIXME: "create" hooks are not triggered
        return dm3c.restc.create_relation(relation)
    }

    /**
     * Deletes a relation from the DB, and from the view (canvas).
     * Note: the canvas and the detail panel are not refreshed.
     *
     * High-level utility method for plugin developers.
     */
    this.delete_relation = function(rel_id) {
        // update DB
        dm3c.restc.delete_relation(rel_id)
        // trigger hook
        dm3c.trigger_hook("post_delete_relation", rel_id)
        // update GUI
        dm3c.hide_relation(rel_id, true)     // is_part_of_delete_operation=true
    }

    /**
     * Hides a relation from the GUI (canvas).
     * Note: the canvas is not refreshed.
     *
     * High-level utility method for plugin developers.
     */
    this.hide_relation = function(rel_id, is_part_of_delete_operation) {
        dm3c.canvas.remove_relation(rel_id, false, is_part_of_delete_operation)
    }



    /*************/
    /*** Types ***/
    /*************/



    /**
     * Creates a topic type in the DB.
     *
     * @param   type_uri        The topic type URI, e.g. "de/deepamehta/core/topictype/Note".        FIXME: update docu
     * @param   properties      Optional: topic properties (object, key: field URI, value: content). FIXME: update docu
     *
     * @return  The topic view of the created topic type.                                            FIXME: update docu
     */
    this.create_topic_type = function(topic_type) {
        // update DB
        var tt = dm3c.restc.create_topic_type(topic_type);
        // trigger hook
        dm3c.trigger_hook("post_create_topic", tt)
        //
        return tt
    }



    /**********************/
    /*** Plugin Support ***/
    /**********************/



    this.register_field_renderer = function(source_path) {
        field_renderer_sources.push(source_path)
    }

    this.css_stylesheet = function(css_path) {
        css_stylesheets.push(css_path)
    }

    this.javascript_source = function(source_path, callback) {
        $.ajax({
            url: source_path,
            dataType: "script",
            success: callback,
            async: callback != undefined
        })
    }

    // ---

    /**
     * Triggers the named hook of all installed plugins.
     *
     * @param   hook_name   Name of the plugin hook to trigger.
     * @param   <varargs>   Variable number of arguments. Passed to the hook.
     */
    this.trigger_hook = function(hook_name) {
        var result = []
        for (var plugin_class in plugins) {
            var plugin = dm3c.get_plugin(plugin_class)
            if (plugin[hook_name]) {
                // 1) Trigger hook
                if (arguments.length == 1) {
                    var res = plugin[hook_name]()
                } else if (arguments.length == 2) {
                    var res = plugin[hook_name](arguments[1])
                } else if (arguments.length == 3) {
                    var res = plugin[hook_name](arguments[1], arguments[2])
                } else if (arguments.length == 4) {
                    var res = plugin[hook_name](arguments[1], arguments[2], arguments[3])
                } else {
                    alert("ERROR (trigger_hook): too much arguments (" +
                        (arguments.length - 1) + "), maximum is 3.\nhook=" + hook_name)
                }
                // 2) Store result
                // Note: undefined is not added to the result, but null is.
                if (res !== undefined) {
                    result.push(res)
                }
            }
        }
        return result
    }

    this.trigger_doctype_hook = function(doc, hook_name, args) {
        // Lookup doctype renderer
        var doctype_impl = dm3c.get_doctype_impl(doc)
        // Trigger the hook only if it is defined (a doctype renderer must not define all hooks).
        if (doctype_impl[hook_name]) {
            return doctype_impl[hook_name](args)
        }
    }

    this.get_plugin = function(plugin_class) {
        return plugins[plugin_class]
    }

    this.get_doctype_impl = function(topic) {
        return doctype_impls[dm3c.type_cache.get(topic.type_uri).js_renderer_class]
    }



    /**************/
    /*** Helper ***/
    /**************/



    // === Topics ===

    this.get_value = function(topic, field_uri) {
        var value = topic.properties[field_uri]
        if (value == undefined) {
            // alert("WARNING (get_value): Data field \"" + field_uri + "\" has no value.\n\n" +
            //    "Topic: " + JSON.stringify(topic))
            value = ""
        }
        return value
    }

    /**
     * Returns the label for the topic.
     *
     * FIXME: method to be dropped? We have this logic at server-side
     */
    this.topic_label = function(topic) {
        var type = dm3c.type_cache.get(topic.type_uri)
        // if there is a topic_label_field_uri declaration use the content of that field
        var field_uri = type.topic_label_field_uri
        if (field_uri) {
            return topic.properties[field_uri] || ""
        }
        // fallback: use the content of the first field
        return topic.properties[type.fields[0].uri] || ""
    }

    // === Types ===

    this.type_label = function(type_uri) {
        return dm3c.type_cache.get_label(type_uri)
    }

    this.reload_types = function() {
        dm3c.type_cache.clear()
        load_types()
    }

    // === Commands ===

    this.get_topic_commands = function(topic, context) {
        return get_commands(dm3c.trigger_hook("add_topic_commands", topic), context)
    }

    this.get_relation_commands = function(relation, context) {
        return get_commands(dm3c.trigger_hook("add_relation_commands", relation), context)
    }

    this.get_canvas_commands = function(cx, cy, context) {
        return get_commands(dm3c.trigger_hook("add_canvas_commands", cx, cy), context)
    }

    // === Persmissions ===

    this.has_write_permission = function(topic) {
        var result = dm3c.trigger_hook("has_write_permission", topic)
        return !js.contains(result, false)
    }

    this.has_create_permission = function(type_uri) {
        var result = dm3c.trigger_hook("has_create_permission", dm3c.type_cache.get(type_uri))
        return !js.contains(result, false)
    }

    // === GUI ===

    /**
     * Reveals a topic that is related to the selected topic.
     */
    this.reveal_related_topic = function(topic_id) {
        // reveal relations
        var relations = dm3c.restc.get_relations(dm3c.selected_topic.id, topic_id)
        for (var i = 0, rel; rel = relations[i]; i++) {
            dm3c.canvas.add_relation(rel.id, rel.src_topic_id, rel.dst_topic_id)
        }
        // reveal topic
        dm3c.add_topic_to_canvas(dm3c.restc.get_topic_by_id(topic_id), "show")
        dm3c.canvas.scroll_topic_to_center(topic_id)
    }

    /**
     * @param   x, y        Optional: the coordinates for placing the topic on the canvas.
     *                      If not specified, placement is up to the canvas.
     */
    this.create_topic_from_menu = function(type_uri, x, y) {
        // 1) update DB
        var topic = dm3c.trigger_hook("custom_create_topic", type_uri)[0]
        if (!topic) {
            topic = dm3c.create_topic(type_uri)
        }
        // 2) update GUI
        dm3c.add_topic_to_canvas(topic, "edit", x, y)
    }

    /**
     * Adds a topic to the canvas, and refreshes the detail panel according to the specified action.
     *
     * High-level utility method for plugin developers.
     * Note: the topic must exist in the DB already. Possibly call create_topic() before.
     *
     * @param   topic       Topic to add (a Topic object).
     * @param   action      Optional: action to perform, 3 possible values:
     *                      "none" - do not select the topic (detail panel doesn't change) -- the default.
     *                      "show" - select the topic and show its info in the detail panel.
     *                      "edit" - select the topic and show its form in the detail panel.
     * @param   x, y        Optional: the coordinates for placing the topic on the canvas.
     *                      If not specified, placement is up to the canvas.
     */
    this.add_topic_to_canvas = function(topic, action, x, y) {
        action = action || "none"   // set default
        // update canvas
        var highlight = action != "none"
        dm3c.canvas.add_topic(topic.id, topic.type_uri, dm3c.topic_label(topic), highlight, true, x, y)
        // update detail panel
        switch (action) {
        case "none":
            break
        case "show":
            dm3c.render_topic(topic.id)
            break
        case "edit":
            dm3c.selected_topic = topic     // update global state
            dm3c.edit_topic(topic)
            break
        default:
            alert("WARNING (add_topic_to_canvas):\n\nUnexpected action: \"" + action + "\"")
        }
    }

    /**
     * Fetches the topic and displays it on the detail panel.
     * Updates global state (selected_topic).
     * If no topic is specified, the selected topic is re-fetched.
     * If there is no selected topic the detail panel is emptied.
     */
    this.render_topic = function(topic_id) {
        if (topic_id == undefined) {
            if (dm3c.selected_topic) {
                topic_id = dm3c.selected_topic.id
            } else {
                dm3c.empty_detail_panel()
                return
            }
        }
        // fetch topic
        var topic = dm3c.restc.get_topic_by_id(topic_id)
        // update global state
        dm3c.selected_topic = topic
        //
        dm3c.trigger_doctype_hook(dm3c.selected_topic, "render_document", dm3c.selected_topic)
    }

    this.edit_topic = function(topic) {
        dm3c.trigger_doctype_hook(topic, "render_form", topic)
    }

    // ---

    this.create_type_menu = function(menu_id, handler) {
        var type_menu = dm3c.ui.menu(menu_id, handler)
        var type_uris = dm3c.type_cache.get_type_uris()
        for (var i = 0; i < type_uris.length; i++) {
            var type_uri = type_uris[i]
            if (dm3c.has_create_permission(type_uri) && !js.contains(EXCLUDE_TYPES_FROM_MENUS, type_uri)) {
                // add type to menu
                type_menu.add_item({
                    label: dm3c.type_label(type_uri),
                    value: type_uri,
                    icon: dm3c.get_icon_src(type_uri)
                })
            }
        }
        return type_menu
    }

    this.recreate_type_menu = function(menu_id) {
        var selection = dm3c.ui.menu_item(menu_id)
        var menu = dm3c.create_type_menu(menu_id)
        // restore selection
        // Note: selection is undefined if the menu has no items.
        if (selection) {
            dm3c.ui.select_menu_item(menu_id, selection.value)
        }
        return menu
    }

    // ---

    /**
     * Adds a menu item to the special menu.
     *
     * @param   item    The menu item to add. An object with this properties:
     *                      "label" - The label to be displayed in the special menu.
     *                      "value" - Optional: the value to be examined by the caller.
     *                          Note: if this item is about to be selected programatically or re-labeled
     *                          the value must be specified.
     */
    this.add_to_special_menu = function(item) {
        var option = $("<option>").text(item.label)
        if (item.value) {
            option.attr("value", item.value)
        }
        $("#special-menu").append(option)
    }

    // --- File upload ---

    /**
     * @param   command     the command (a string) send to the server along with the selected file.
     * @param   callback    the function that is invoked once the file has been uploaded and processed at server-side.
     *                      One argument is passed to that function: the object (deserialzed JSON) returned by the
     *                      (server-side) executeCommandHook.
     */
    this.show_upload_dialog = function(command, callback) {
        $("#upload-dialog-command").attr("value", command)
        $("#upload-dialog").dialog("open")
        // bind callback function, using artifact ID as event namespace
        $("#upload-target").unbind("load.deepamehta3-client")
        $("#upload-target").bind("load.deepamehta3-client", upload_complete(callback))

        function upload_complete(callback) {
            return function() {
                $("#upload-dialog").dialog("close")
                // Note: iframes (the upload target) must be DOM manipulated as frames
                var result = $("pre", window.frames["upload-target"].document).text()
                try {
                    callback(JSON.parse(result))
                } catch (e) {
                    alert("No valid server response: " + result + "\n(" + JSON.stringify(e) + ")")
                }
            }
        }
    }

    // --- Image Tracker ---

    var image_tracker

    this.create_image_tracker = function(callback_func) {

        return image_tracker = new ImageTracker()

        function ImageTracker() {

            var types = []      // topic types whose images are tracked

            this.add_type = function(type) {
                if (!js.contains(types, type)) {
                    types.push(type)
                }
            }

            // Checks if the tracked images are loaded completely.
            // If so, the callback is triggered and this tracker is removed.
            this.check = function() {
                if (types.every(function(type) {return dm3c.get_type_icon(type).complete})) {
                    callback_func()
                    image_tracker = undefined
                }
            }
        }
    }

    // ---

    /**
     * @param   topics      Topics to render (array of Topic objects).
     */
    this.render_topic_list = function(topics) {
        var table = $("<table>")
        for (var i = 0, topic; topic = topics[i]; i++) {
            // icon
            var icon_td = $("<td>").addClass("topic-icon").addClass(i == topics.length - 1 ? "last-topic" : undefined)
            icon_td.append(render_topic_anchor(topic, type_icon_tag(topic.type_uri, "type-icon")))
            // label
            var topic_td = $("<td>").addClass("topic-label").addClass(i == topics.length - 1 ? "last-topic" : undefined)
            var list_item = $("<div>").append(render_topic_anchor(topic, topic.label))
            dm3c.trigger_hook("render_topic_list_item", topic, list_item)
            topic_td.append(list_item)
            //
            table.append($("<tr>").append(icon_td).append(topic_td))
        }
        return table
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    this.image_tag = function(src, css_class) {
        return $("<img>").attr("src", src).addClass(css_class)
    }

    /**
     * Returns the icon source for a topic type.
     * If no icon is configured for that type the source of the generic topic icon is returned.
     *
     * @return  The icon source (string).
     */
    this.get_icon_src = function(type_uri) {
        // Note: topic_type is undefined if plugin is deactivated and content still exist.
        var topic_type = dm3c.type_cache.get(type_uri)
        if (topic_type && topic_type.icon_src) {
            return topic_type.icon_src
        } else {
            return GENERIC_TOPIC_ICON_SRC
        }
    }

    /**
     * Returns the icon for a topic type.
     * If no icon is configured for that type the generic topic icon is returned.
     *
     * @return  The icon (JavaScript Image object)
     */
    this.get_type_icon = function(type_uri) {
        return dm3c.type_cache.get_icon(type_uri) || generic_topic_icon
    }

    this.create_image = function(src) {
        var img = new Image()
        img.src = src   // Note: if src is a relative URL JavaScript extends img.src to an absolute URL
        img.onload = function(arg0) {
            // Note: "this" is the image. The argument is the "load" event.
            if (LOG_IMAGE_LOADING) dm3c.log("Image ready: " + src)
            notify_image_trackers()
        }
        return img
    }

    // ---

    this.empty_detail_panel = function() {
        $("#detail-panel").empty()
        $("#lower-toolbar").empty()
    }

    // ---

    this.log = function(text) {
        if (ENABLE_LOGGING) {
            // Note: the log window might be closed meanwhile,
            // or it might not apened at all due to browser security restrictions.
            if (log_window && log_window.document) {
                log_window.document.writeln(js.render_text(text) + "<br>")
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === GUI ===

    function searchmode_select() {
        return $("<select>").attr("id", "searchmode-select")
    }

    function searchmode_selected(menu_item) {
        // Note: we must empty the current search widget _before_ the new search widget is build. Otherwise the
        // search widget's event handlers might get lost.
        // Consider this case: the "by Type" searchmode is currently selected and the user selects it again. The
        // ui_menu() call for building the type menu will unnecessarily add the menu to the DOM because it finds
        // an element with the same ID on the page. A subsequent empty() would dispose the just added type menu
        // -- including its event handlers -- and the append() would eventually add the crippled type menu.
        $("#search-widget").empty()
        var searchmode = menu_item.label
        var search_widget = dm3c.trigger_hook("search_widget", searchmode)[0]
        $("#search-widget").append(search_widget)
    }

    function search() {
        try {
            var searchmode = dm3c.ui.menu_item("searchmode-select").label
            var search_topic = dm3c.trigger_hook("search", searchmode)[0]
            dm3c.add_topic_to_canvas(search_topic, "show")
        } catch (e) {
            alert("ERROR while searching:\n\n" + JSON.stringify(e))
        }
        return false
    }

    // --- Special Menu ---

    function create_special_select() {
        return $("<select>").attr("id", "special-menu")
    }

    function special_selected(menu_item) {
        var command = menu_item.label
        dm3c.trigger_hook("handle_special_command", command)
    }

    // --- Rendering ---

    /**
     * @param   topic       Topic to render (a Topic object).
     */
    function render_topic_anchor(topic, anchor_content) {
        return $("<a>").attr({href: "#"}).append(anchor_content).click(function() {
            dm3c.reveal_related_topic(topic.id)
            return false
        })
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    function type_icon_tag(type_uri, css_class) {
        return dm3c.image_tag(dm3c.get_icon_src(type_uri), css_class)
    }

    // ---

    function submit_document() {
        var submit_button = $("#document-form button[submit=true]")
        // alert("submit_document: submit button id=" + submit_button.attr("id"))
        submit_button.click()
        return false
    }

    // === Plugin Support ===

    function register_plugin(source_path) {
        plugin_sources.push(source_path)
    }

    function register_doctype_renderer(source_path) {
        doctype_impl_sources.push(source_path)
    }

    // ---

    /**
     * Registers server-side plugins to the list of plugins to load at client-side.
     */
    function register_plugins() {
        var plugins = dm3c.restc.get_plugins()
        if (LOG_PLUGIN_LOADING) dm3c.log("Plugins installed at server-side: " + plugins.length)
        for (var i = 0, plugin; plugin = plugins[i]; i++) {
            if (plugin.plugin_file) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... plugin \"" + plugin.plugin_id +
                    "\" contains client-side parts -- to be loaded")
                register_plugin("/" + plugin.plugin_id + "/script/" + plugin.plugin_file)
            } else {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... plugin \"" + plugin.plugin_id +
                    "\" contains no client-side parts -- nothing to load")
            }
        }
    }

    // ---

    function get_commands(cmd_lists, context) {
        var commands = []
        for (var i = 0, cmds; cmds = cmd_lists[i]; i++) {
            for (var j = 0, cmd; cmd = cmds[j]; j++) {
                if (cmd.context == context) {
                    commands.push(cmd)
                }
            }
        }
        return commands
    }

    // --- Types ---

    function load_types() {
        var type_uris = dm3c.restc.get_topic_type_uris()
        for (var i = 0; i < type_uris.length; i++) {
            var type_uri = type_uris[i]
            var type = dm3c.restc.get_topic_type(type_uri)
            dm3c.type_cache.put(type_uri, type)
        }
    }

    function notify_image_trackers() {
        image_tracker && image_tracker.check()
    }

    // ------------------------------------------------------------------------------------------------ Constructor Code

    // --- register default modules ---
    //
    register_doctype_renderer("script/document-renderers/plain_document.js")
    //
    this.register_field_renderer("script/datafield-renderers/text_field_renderer.js")
    this.register_field_renderer("script/datafield-renderers/number_field_renderer.js")
    this.register_field_renderer("script/datafield-renderers/date_field_renderer.js")
    this.register_field_renderer("script/datafield-renderers/html_field_renderer.js")
    this.register_field_renderer("script/datafield-renderers/reference_field_renderer.js")
    //
    this.register_field_renderer("script/datafield-renderers/title_renderer.js")
    this.register_field_renderer("script/datafield-renderers/body_text_renderer.js")
    //
    register_plugin("script/internal-plugins/default_plugin.js")
    register_plugin("script/internal-plugins/fulltext_plugin.js")
    register_plugin("script/internal-plugins/tinymce_plugin.js")

    var generic_topic_icon = this.create_image(GENERIC_TOPIC_ICON_SRC)

    $(function() {
        //
        // --- 1) Prepare GUI ---
        $("#upper-toolbar").addClass("ui-widget-header").addClass("ui-corner-all")
        // the search form
        $("#searchmode-select-placeholder").replaceWith(searchmode_select())
        $("#search_field").attr({size: dm3c.SEARCH_FIELD_WIDTH})
        $("#search-form").submit(search)
        dm3c.ui.button("search-button", search, "Search", "gear")
        // the special form
        $("#special-menu-placeholder").replaceWith(create_special_select())
        // the document form
        $("#document-form").submit(submit_document)
        detail_panel_width = $("#detail-panel").width()
        if (dm3c.LOG_GUI) dm3c.log("Mesuring detail panel width: " + detail_panel_width)
        // the upload dialog
        $("#upload-dialog").dialog({
            modal: true, autoOpen: false, draggable: false, resizable: false, width: UPLOAD_DIALOG_WIDTH
        })
        //
        // --- 2) Load Plugins ---
        // Note: in order to let a plugin DOM manipulate the GUI the plugins are loaded *after* the GUI is prepared.
        extend_rest_client()
        load_types()
        //
        register_plugins()
        load_plugins()
        load_document_renderers()
        load_field_renderers()
        load_stylesheets()
        //
        // --- 3) Setup GUI ---
        // Note: in order to let a plugin provide a custom canvas renderer (the dm3-freifunk-geomap plugin does!)
        // the canvas is created *after* loading the plugins.
        dm3c.canvas = dm3c.trigger_hook("get_canvas_renderer")[0] || new Canvas()
        // Note: in order to let a plugin provide the initial canvas rendering (the deepamehta3-topicmaps plugin does!)
        // the "init" hook is triggered *after* creating the canvas.
        dm3c.trigger_hook("init")
        //
        // setup create widget
        var menu = dm3c.create_type_menu("create-type-menu")
        $("#create-type-menu-placeholder").replaceWith(menu.dom)
        dm3c.ui.button("create-button", do_create_topic, "Create", "plus")
        if (!menu.get_item_count()) {
            $("#create-widget").hide()
        }
        //
        dm3c.ui.menu("searchmode-select", searchmode_selected)
        dm3c.ui.menu("special-menu", special_selected, undefined, "Special")
        // the detail panel
        if (dm3c.LOG_GUI) dm3c.log("Setting detail panel height: " + $("#canvas").height())
        $("#detail-panel").height($("#canvas").height())
        //
        $(window).resize(window_resized)

        /**
         * Loads and instantiates all registered plugins.
         */
        function load_plugins() {

            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + plugin_sources.length + " plugins:")
            for (var i = 0, plugin_source; plugin_source = plugin_sources[i]; i++) {
                load_plugin(plugin_source)
            }

            function load_plugin(plugin_source) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + plugin_source)
                // 1) load
                dm3c.javascript_source(plugin_source)
                // 2) instantiate
                var plugin_class = js.basename(plugin_source)
                if (LOG_PLUGIN_LOADING) dm3c.log(".......... instantiating \"" + plugin_class + "\"")
                plugins[plugin_class] = js.new_object(plugin_class)
            }
        }

        function load_document_renderers() {

            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + doctype_impl_sources.length + " doctype renderers:")
            for (var i = 0, doctype_impl_src; doctype_impl_src = doctype_impl_sources[i]; i++) {
                load_doctype_impl(doctype_impl_src)
            }

            function load_doctype_impl(doctype_impl_src) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + doctype_impl_src)
                //
                // Note: the PlainDocument renderer can't be loaded dynamically because of a strange Firefox problem.
                // If script is loaded synchronously: instantiation fails ("PlainDocument is not defined").
                // If script is loaded asynchronously (with a callback): instantiation fails silently.
                // The workaround is to load plain_document.js statically (see index.html).
                // With Safari their is no problem at all.
                //
                // 1) load
                // dm3c.javascript_source(doctype_impl_src)
                //
                // 2) instantiate
                var doctype_class = js.to_camel_case(js.basename(doctype_impl_src))
                if (LOG_PLUGIN_LOADING) dm3c.log(".......... instantiating \"" + doctype_class + "\"")
                if (LOG_PLUGIN_LOADING) dm3c.log("...........typeof=" + typeof(eval(doctype_class)))
                doctype_impls[doctype_class] = js.new_object(doctype_class)
            }
        }

        function load_field_renderers() {
            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + field_renderer_sources.length + " data field renderers:")
            for (var i = 0, field_renderer_source; field_renderer_source = field_renderer_sources[i]; i++) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + field_renderer_source)
                dm3c.javascript_source(field_renderer_source, function() {})
            }
        }

        function load_stylesheets() {
            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + css_stylesheets.length + " CSS stylesheets:")
            for (var i = 0, css_stylesheet; css_stylesheet = css_stylesheets[i]; i++) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + css_stylesheet)
                $("head").append($("<link>").attr({rel: "stylesheet", href: css_stylesheet, type: "text/css"}))
            }
        }

        function do_create_topic() {
            var type_uri = dm3c.ui.menu_item("create-type-menu").value
            dm3c.create_topic_from_menu(type_uri)
        }

        function window_resized() {
            dm3c.canvas.adjust_size()
            $("#detail-panel").height($("#canvas").height())
        }

        function extend_rest_client() {

            dm3c.restc.search_topics_and_create_bucket = function(text, field_uri, whole_word) {
                var params = this.createRequestParameter({search: text, field: field_uri, wholeword: whole_word})
                return this.request("GET", "/client/search" + params.to_query_string())
            }

            // Note: this method is actually part of the Type Search plugin.
            // TODO: proper modulariuation. Either let the Type Search plugin provide its own REST resource (with
            // another namespace again) or make the Type Search plugin an integral part of the Client plugin.
            dm3c.restc.get_topics_and_create_bucket = function(type_uri) {
                return this.request("GET", "/client/search/by_type/" + encodeURIComponent(type_uri))
            }
        }
    })
}
