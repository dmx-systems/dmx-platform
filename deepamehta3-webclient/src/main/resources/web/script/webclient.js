var dm3c = new function() {

    var CORE_SERVICE_URI = "/core"
    this.SEARCH_FIELD_WIDTH = 16    // in chars
    this.COMPOSITE_PATH_SEPARATOR = "/"
    var UPLOAD_DIALOG_WIDTH = "50em"
    this.GENERIC_TOPIC_ICON_SRC = "images/ball-grey.png"

    var ENABLE_LOGGING = false
    var LOG_PLUGIN_LOADING = false
    var LOG_IMAGE_LOADING = false
    this.LOG_GUI = false

    this.restc = new RESTClient(CORE_SERVICE_URI)
    this.type_cache = new TypeCache()
    this.ui = new UIHelper()
    this.render = new RenderHelper()

    this.selected_object = null // object being selected (Topic or Association object), or null if there is no selection
    this.canvas = null          // the canvas that displays the topicmap (a Canvas object)
    this.page_panel = null      // the page panel on the right hand side (a PagePanel object)
    //
    var plugin_sources = []
    var plugins = {}                // key: plugin class name, base name of source file (string), value: plugin instance
    var page_renderer_sources = []
    var page_renderers = {}         // key: page renderer class name, camel case (string), value: renderer instance
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
     * @param   type_uri        The topic type URI, e.g. "dm3.notes.note".
     * @param   composite       Optional.
     *
     * @return  The topic as stored in the DB.
     */
    this.create_topic = function(type_uri, composite) {
        var topic_model = {
            // Note: "uri", "value", and "composite" are optional
            type_uri: type_uri,
            composite: composite    // not serialized to request body if undefined
        }
        // 1) update DB
        var topic = build_topic(dm3c.restc.create_topic(topic_model))
        // alert("Topic created: " + JSON.stringify(topic));
        // 2) trigger hook
        dm3c.trigger_plugin_hook("post_create_topic", topic)
        //
        return topic
    }

    /**
     * Updates a topic in the DB.
     * Triggers the "post_update_topic" hook.
     *
     * High-level utility method for plugin developers.
     *
     * @param   old_topic   the topic that is about to be updated. This "old" version is passed to the
     *                      post_update_topic hook to let plugins compare the old and new ones.
     * @param   new_topic   the new topic that is about to override the old topic.
     *
     * @return  The updated topic as stored in the DB.
     *          Note: the new topic and the updated topic are not necessarily 100% identical. The new topic contains
     *          only the parts that are required for the update, e.g. a composite topic doesn't contain the "value"
     *          field. The updated topic on the other hand is the complete topic as returned by the server.
     */
    this.update_topic = function(old_topic, new_topic) {
        // 1) update DB
        // alert("dm3c.update_topic(): new_topic=" + JSON.stringify(new_topic));
        var updated_topic = build_topic(dm3c.restc.update_topic(new_topic))
        // 2) trigger hook
        dm3c.trigger_plugin_hook("post_update_topic", updated_topic, old_topic)
        //
        return updated_topic
    }

    /**
     * Deletes a topic (including its associations) from the DB and the GUI, and triggers the "post_delete_topic" hook.
     *
     * High-level utility method for plugin developers.
     */
    this.delete_topic = function(topic) {
        // update DB
        dm3c.restc.delete_topic(topic.id)
        // trigger hook
        dm3c.trigger_plugin_hook("post_delete_topic", topic)
        // update GUI
        dm3c.hide_topic(topic.id, true)      // is_part_of_delete_operation=true
    }

    /**
     * Hides a topic (including its associations) from the GUI (canvas & page panel).
     *
     * High-level utility method for plugin developers.
     * FIXME: remove is_part_of_delete_operation parameter
     */
    this.hide_topic = function(topic_id, is_part_of_delete_operation) {
        // 1) update canvas
        dm3c.canvas.remove_all_associations_of_topic(topic_id, is_part_of_delete_operation)
        dm3c.canvas.remove_topic(topic_id, true, is_part_of_delete_operation)       // refresh_canvas=true
        // 2) update page panel
        if (topic_id == dm3c.selected_object.id) {
            // update model
            dm3c.selected_object = null
            // update GUI
            dm3c.page_panel.clear()
        } else {
            alert("WARNING: removed topic which was not selected\n" +
                "(removed=" + topic_id + " selected=" + dm3c.selected_object.id + ")")
        }
    }



    /********************/
    /*** Associations ***/
    /********************/



    /**
     * Creates an association in the DB.
     *
     * High-level utility method for plugin developers.
     *
     * @param   type_uri            The association type URI, e.g. "dm3.core.instantiation".
     * @param   role_1              The topic role or association role at one end (an object).
     *                              Examples for a topic role:
     *                                  {topic_uri: "dm3.core.cardinality", role_type_uri: "dm3.core.type"},
     *                                  {topic_id: 123,                     role_type_uri: "dm3.core.instance"},
     *                              The topic can be identified either by URI or by ID.
     *                              Example for an association role:
     *                                  {assoc_id: 456, role_type_uri: "dm3.core.assoc_def"},
     *                              The association is identified by ID.
     * @param   role_2              The topic role or association role at the other end (an object, like role_1).
     *
     * @return  The association as stored in the DB.
     */
    this.create_association = function(type_uri, role_1, role_2) {
        var assoc_model = {
            type_uri: type_uri,
            role_1: role_1,
            role_2: role_2
        }
        // FIXME: "create" hooks are not triggered
        return dm3c.restc.create_association(assoc_model)
    }

    /**
     * Updates an association in the DB.
     * ### FIXME: Triggers the "post_update_topic" hook.
     *
     * High-level utility method for plugin developers.
     *
     * @param   old_assoc   the association that is about to be updated. ### FIXME: This "old" version is passed to the
     *                      post_update_topic hook to let plugins compare the old and new ones.
     * @param   new_assoc   the new association that is about to override the old association.
     *
     * @return  The updated association as stored in the DB.
     */
    this.update_association = function(old_assoc, new_assoc) {
        // 1) update DB
        // alert("dm3c.update_association(): new_assoc=" + JSON.stringify(new_assoc));
        var updated_assoc = build_association(dm3c.restc.update_association(new_assoc))
        // 2) trigger hook
        // ### dm3c.trigger_plugin_hook("post_update_topic", updated_assoc, old_assoc)
        //
        return updated_assoc
    }

    /**
     * Deletes an association from the DB, and from the view (canvas).
     * Note: the canvas and the page panel are not refreshed.
     *
     * High-level utility method for plugin developers.
     */
    this.delete_association = function(assoc_id) {
        // update DB
        dm3c.restc.delete_association(assoc_id)
        // trigger hook
        dm3c.trigger_plugin_hook("post_delete_relation", assoc_id)
        // update GUI
        dm3c.hide_association(assoc_id, true)     // is_part_of_delete_operation=true
    }

    /**
     * Hides an association from the GUI (canvas).
     * Note: the canvas is not refreshed.
     *
     * High-level utility method for plugin developers (### the is_part_of_delete_operation parameter is not!).
     */
    this.hide_association = function(assoc_id, is_part_of_delete_operation) {
        dm3c.canvas.remove_association(assoc_id, false, is_part_of_delete_operation)  // refresh_canvas=false
    }



    /*************/
    /*** Types ***/
    /*************/



    /**
     * Creates a topic type in the DB.
     */
    this.create_topic_type = function(topic_type) {
        // 1) update DB
        var created_topic_type = build_topic_type(dm3c.restc.create_topic_type(topic_type))
        // alert("Topic type created: " + JSON.stringify(topic_type));
        // 2) trigger hook
        dm3c.trigger_plugin_hook("post_create_topic", created_topic_type)
        //
        return created_topic_type
    }

    /**
     * Updates a topic type in the DB.
     */
    this.update_topic_type = function(old_topic_type, new_topic_type) {
        // 1) update DB
        var updated_topic_type = build_topic_type(dm3c.restc.update_topic_type(new_topic_type))
        // alert("Topic type updated: " + JSON.stringify(topic_type));
        // 2) trigger hook
        dm3c.trigger_plugin_hook("post_update_topic", updated_topic_type, old_topic_type)
        //
        return updated_topic_type
    }



    /**********************/
    /*** Plugin Support ***/
    /**********************/



    this.register_page_renderer = function(source_path) {
        page_renderer_sources.push(source_path)
    }

    this.register_field_renderer = function(source_path) {
        field_renderer_sources.push(source_path)
    }

    this.register_css_stylesheet = function(css_path) {
        css_stylesheets.push(css_path)
    }

    /**
     * Loads a Javascript file dynamically. Synchronous and asynchronous loading is supported.
     *
     * @param   script_url      The URL (absolute or relative) of the Javascript file to load.
     * @param   callback        The function to invoke when asynchronous loading is complete.
     *                          If not given loading is performed synchronously.
     */
    this.javascript_source = function(script_url, callback) {
        $.ajax({
            url: script_url,
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
    this.trigger_plugin_hook = function(hook_name) {
        var result = []
        for (var plugin_class in plugins) {
            var plugin = dm3c.get_plugin(plugin_class)
            if (plugin[hook_name]) {
                // 1) Trigger hook ### FIXME: use apply()
                if (arguments.length == 1) {
                    var res = plugin[hook_name]()
                } else if (arguments.length == 2) {
                    var res = plugin[hook_name](arguments[1])
                } else if (arguments.length == 3) {
                    var res = plugin[hook_name](arguments[1], arguments[2])
                } else if (arguments.length == 4) {
                    var res = plugin[hook_name](arguments[1], arguments[2], arguments[3])
                } else {
                    alert("ERROR (trigger_plugin_hook): too much arguments (" +
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

    this.trigger_page_renderer_hook = function(topic_or_association, hook_name, args) {
        // Lookup page renderer
        var page_renderer = dm3c.get_page_renderer(topic_or_association)
        // Trigger the hook only if it is defined (a page renderer must not define all hooks).
        if (page_renderer[hook_name]) {
            return page_renderer[hook_name](args)
        }
    }

    this.get_plugin = function(plugin_class) {
        return plugins[plugin_class]
    }

    this.get_page_renderer = function(topic_or_association_or_classname) {
        if (typeof(topic_or_association_or_classname) == "string") {
            var page_renderer_class = topic_or_association_or_classname
        } else {
            var type = topic_or_association_or_classname.get_type()
            var page_renderer_class = type.get_page_renderer_class()
        }
        var page_renderer = page_renderers[page_renderer_class]
        // error check
        if (!page_renderer) {
            throw "UnknownPageRenderer: page renderer \"" + page_renderer_class + "\" is not registered"
        }
        //
        return page_renderer
    }



    /**************/
    /*** Helper ***/
    /**************/



    // === Topics ===

    this.hash_by_type = function(topics) {
        var hashed_topics = {}
        for (var i = 0, topic; topic = topics[i]; i++) {
            hashed_topics[topic.type_uri] = topic
        }
        return hashed_topics
    }

    // === Types ===

    /**
     * Convenience method that returns the topic type's label.
     */
    this.type_label = function(type_uri) {
        return dm3c.type_cache.get_topic_type(type_uri).value
    }

    this.reload_types = function() {
        dm3c.type_cache.clear()
        load_types()
    }

    /**
     * Convenience method that returns the topic type's icon source.
     * If no icon is configured the generic topic icon is returned.
     *
     * @return  The icon source (string).
     */
    this.get_icon_src = function(type_uri) {
        var topic_type = dm3c.type_cache.get_topic_type(type_uri)
        // Note: topic_type is undefined if plugin is deactivated and content still exist.
        if (topic_type) {
            return topic_type.get_icon_src()
        }
        return this.GENERIC_TOPIC_ICON_SRC
    }

    /**
     * Read out a view configuration setting.
     * <p>
     * Compare to server-side counterparts: WebclientPlugin.getViewConfig() and ViewConfiguration.getSetting()
     *
     * @param   configurable    A topic type, an association type or an association definition.
     *                          Must not be null/undefined.
     * @param   setting         Last component of the setting URI, e.g. "icon_src".
     *
     * @return  The setting value, or <code>undefined</code> if there is no such setting
     */
    this.get_view_config = function(configurable, setting) {
        // error check
        if (!configurable.view_config_topics) {
            throw "InvalidConfigurable: no \"view_config_topics\" property found in " + JSON.stringify(configurable)
        }
        // every configurable has an view_config_topics object, however it might be empty
        var view_config = configurable.view_config_topics["dm3.webclient.view_config"]
        if (view_config) {
            return view_config.composite["dm3.webclient." + setting]
        }
    }

    // === Commands ===

    this.get_topic_commands = function(topic, context) {
        return get_commands(dm3c.trigger_plugin_hook("add_topic_commands", topic), context)
    }

    this.get_association_commands = function(assoc, context) {
        return get_commands(dm3c.trigger_plugin_hook("add_association_commands", assoc), context)
    }

    this.get_canvas_commands = function(cx, cy, context) {
        return get_commands(dm3c.trigger_plugin_hook("add_canvas_commands", cx, cy), context)
    }

    // === Persmissions ===

    // ### TODO: handle associations as well
    this.has_write_permission = function(topic) {
        var result = dm3c.trigger_plugin_hook("has_write_permission", topic)
        return !js.contains(result, false)
    }

    // ### TODO: handle association types as well
    this.has_create_permission = function(type_uri) {
        var result = dm3c.trigger_plugin_hook("has_create_permission", dm3c.type_cache.get_topic_type(type_uri))
        return !js.contains(result, false)
    }

    // === GUI ===

    /**
     * Reveals a topic that is related to the selected topic.
     */
    this.reveal_related_topic = function(topic_id) {
        // reveal associations
        var assocs = dm3c.restc.get_associations(dm3c.selected_object.id, topic_id)
        for (var i = 0, assoc; assoc = assocs[i]; i++) {
            dm3c.canvas.add_association(assoc)
        }
        // reveal topic
        dm3c.add_topic_to_canvas(dm3c.fetch_topic(topic_id), "show")
        dm3c.canvas.scroll_topic_to_center(topic_id)
    }

    /**
     * @param   x, y        Optional: the coordinates for placing the topic on the canvas.
     *                      If not specified, placement is up to the canvas.
     */
    this.create_topic_from_menu = function(type_uri, x, y) {
        // 1) update DB
        topic = dm3c.create_topic(type_uri)
        // ### alert("topic created: " + JSON.stringify(topic))
        // 2) update GUI
        dm3c.add_topic_to_canvas(topic, "edit", x, y)
    }

    /**
     * Adds a topic to the canvas, and refreshes the page panel according to the specified action.
     *
     * High-level utility method for plugin developers.
     * Note: the topic must exist in the DB already. Possibly call create_topic() before.
     *
     * @param   topic       Topic to add (a Topic object).
     * @param   action      Optional: action to perform, 3 possible values:
     *                      "none" - do not select the topic (page panel doesn't change) -- the default.
     *                      "show" - select the topic and show its info in the page panel.
     *                      "edit" - select the topic and show its form in the page panel.
     * @param   x, y        Optional: the coordinates for placing the topic on the canvas.
     *                      If not specified, placement is up to the canvas.
     */
    this.add_topic_to_canvas = function(topic, action, x, y) {
        action = action || "none"   // set default
        // update canvas
        var highlight = action != "none"
        dm3c.canvas.add_topic(topic, highlight, true, x, y)
        // update page panel
        switch (action) {
        case "none":
            break
        case "show":
            display_object(topic)
            break
        case "edit":
            dm3c.begin_editing(topic)
            break
        default:
            alert("WARNING (add_topic_to_canvas):\n\nUnexpected action: \"" + action + "\"")
        }
    }

    // ---

    /**
     * Fetches the topic and displays it on the page panel.
     */
    this.select_topic = function(topic_id) {
        display_object(dm3c.fetch_topic(topic_id))
    }

    /**
     * Fetches the association and displays it on the page panel.
     */
    this.select_association = function(assoc_id) {
        display_object(dm3c.fetch_association(assoc_id))
    }

    // ---

    /**
     * Displays the topic or association on the page panel.
     */
    function display_object(object) {
        // update model
        set_selected_object(object)
        // update GUI
        dm3c.page_panel.display(object)
    }

    this.begin_editing = function(object) {
        // update model
        set_selected_object(object)
        // update GUI
        dm3c.page_panel.edit(object)
    }

    // ---

    /**
     * @param   menu_id     Used IDs are e.g.
     *                      "create-type-menu"
     *                      "search-type-menu" - introduced by typesearch plugin
     *
     * @return  The menu (a UIHelper Menu object).
     */
    this.create_type_menu = function(menu_id, handler) {
        var type_menu = dm3c.ui.menu(menu_id, handler)
        var type_uris = dm3c.type_cache.get_type_uris()
        for (var i = 0; i < type_uris.length; i++) {
            var type_uri = type_uris[i]
            var topic_type = dm3c.type_cache.get_topic_type(type_uri)
            if (dm3c.has_create_permission(type_uri) && topic_type.get_menu_config(menu_id)) {
                // add type to menu
                type_menu.add_item({
                    label: topic_type.value,
                    value: type_uri,
                    icon: topic_type.get_icon_src()
                })
            }
        }
        //
        dm3c.trigger_plugin_hook("post_create_type_menu", type_menu)
        //
        return type_menu
    }

    /**
     * @param   menu_id     Used IDs are e.g.
     *                      "create-type-menu"
     *                      "search-type-menu" - introduced by typesearch plugin
     *
     * @return  The menu (a UIHelper Menu object).
     */
    this.recreate_type_menu = function(menu_id) {
        var selection = dm3c.ui.menu_item(menu_id)
        var type_menu = dm3c.create_type_menu(menu_id)
        // restore selection
        // Note: selection is undefined if the menu has no items.
        if (selection) {
            dm3c.ui.select_menu_item(menu_id, selection.value)
        }
        return type_menu
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
        $("#upload-dialog form").attr("action", "/core/command/" + command)
        $("#upload-dialog").dialog("open")
        // bind callback function, using artifact ID as event namespace
        $("#upload-target").unbind("load.deepamehta3-webclient")
        $("#upload-target").bind("load.deepamehta3-webclient", upload_complete(callback))

        function upload_complete(callback) {
            return function() {
                $("#upload-dialog").dialog("close")
                // Note: iframes (the upload target) must be DOM manipulated as frames
                var result = $("pre", window.frames["upload-target"].document).text()
                try {
                    callback(JSON.parse(result))
                } catch (e) {
                    alert("No valid server response: \"" + result + "\"\n\nException=" + JSON.stringify(e))
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

    this.fetch_topic = function(topic_id) {
        return build_topic(dm3c.restc.get_topic_by_id(topic_id))
    }

    this.fetch_association = function(assoc_id) {
        return build_association(dm3c.restc.get_association(assoc_id))
    }

    function fetch_topic_type(topic_type_uri) {
        return build_topic_type(dm3c.restc.get_topic_type(topic_type_uri))
    }

    function fetch_association_type(assoc_type_uri) {
        return build_association_type(dm3c.restc.get_association_type(assoc_type_uri))
    }

    // ---

    function build_topic(topic) {
        return new Topic(topic)
    }

    function build_association(assoc) {
        return new Association(assoc)
    }

    function build_topic_type(topic_type) {
        return new TopicType(topic_type)
    }

    function build_association_type(assoc_type) {
        return new AssociationType(assoc_type)
    }

    // === Model ===

    function set_selected_object(object) {
        dm3c.selected_object = object
    }

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
        var search_widget = dm3c.trigger_plugin_hook("search_widget", searchmode)[0]
        $("#search-widget").append(search_widget)
    }

    function search() {
        try {
            var searchmode = dm3c.ui.menu_item("searchmode-select").label
            var search_topic = build_topic(dm3c.trigger_plugin_hook("search", searchmode)[0])
            // alert("search_topic=" + JSON.stringify(search_topic))
            dm3c.add_topic_to_canvas(search_topic, "show")
        } catch (e) {
            alert("ERROR while searching:\n\n" + JSON.stringify(e))
        }
        return false
    }

    // ---

    // ### TODO: rework
    function submit_document() {
        var submit_button = $("#page-panel button[submit=true]")
        // alert("submit_document: submit button id=" + submit_button.attr("id"))
        submit_button.click()
        return false
    }

    // --- Special Menu ---

    function create_special_select() {
        return $("<select>").attr("id", "special-menu")
    }

    function special_selected(menu_item) {
        var command = menu_item.label
        dm3c.trigger_plugin_hook("handle_special_command", command)
    }

    // === Plugin Support ===

    function register_plugin(source_path) {
        plugin_sources.push(source_path)
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
        // topic types
        var type_uris = dm3c.restc.get_topic_type_uris()
        for (var i = 0; i < type_uris.length; i++) {
            dm3c.type_cache.put_topic_type(fetch_topic_type(type_uris[i]))
        }
        // association types
        var type_uris = dm3c.restc.get_association_type_uris()
        for (var i = 0; i < type_uris.length; i++) {
            dm3c.type_cache.put_association_type(fetch_association_type(type_uris[i]))
        }
    }

    function notify_image_trackers() {
        image_tracker && image_tracker.check()
    }

    // ------------------------------------------------------------------------------------------------ Constructor Code

    // --- register default modules ---
    //
    this.register_page_renderer("script/page-renderers/topic_renderer.js")
    this.register_page_renderer("script/page-renderers/association_renderer.js")
    //
    this.register_field_renderer("script/field-renderers/text_field_renderer.js")
    this.register_field_renderer("script/field-renderers/number_field_renderer.js")
    this.register_field_renderer("script/field-renderers/date_field_renderer.js")
    this.register_field_renderer("script/field-renderers/html_field_renderer.js")
    this.register_field_renderer("script/field-renderers/reference_field_renderer.js")
    //
    this.register_field_renderer("script/field-renderers/title_renderer.js")
    this.register_field_renderer("script/field-renderers/body_text_renderer.js")
    this.register_field_renderer("script/field-renderers/search_result_renderer.js")
    //
    register_plugin("script/internal-plugins/default_plugin.js")
    register_plugin("script/internal-plugins/fulltext_plugin.js")
    register_plugin("script/internal-plugins/tinymce_plugin.js")

    var generic_topic_icon = this.create_image(this.GENERIC_TOPIC_ICON_SRC)

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
        // the page panel
        dm3c.page_panel = new PagePanel()
        $("#split-panel > tbody > tr > td").eq(1).append(dm3c.page_panel.dom)
        // ### $("#document-form").submit(submit_document)
        detail_panel_width = $("#page-content").width()
        if (dm3c.LOG_GUI) dm3c.log("Mesuring page panel width: " + detail_panel_width)
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

        /**
         * Loads and instantiates all registered plugins.
         */
        function load_plugins() {

            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + plugin_sources.length + " plugins:")
            var plugins_complete = 0
            for (var i = 0, plugin_source; plugin_source = plugin_sources[i]; i++) {
                load_plugin(plugin_source)
            }

            function load_plugin(plugin_source) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + plugin_source)
                // load plugin asynchronously
                dm3c.javascript_source(plugin_source, function() {
                    // instantiate
                    var plugin_class = js.basename(plugin_source)
                    if (LOG_PLUGIN_LOADING) dm3c.log(".......... instantiating \"" + plugin_class + "\"")
                    plugins[plugin_class] = js.new_object(plugin_class)
                    // all plugins complete?
                    plugins_complete++
                    if (plugins_complete == plugin_sources.length) {
                        if (LOG_PLUGIN_LOADING) dm3c.log("PLUGINS COMPLETE!")
                        setup_gui()
                    }
                })
            }
        }

        function setup_gui() {
            load_page_renderers()
            load_field_renderers()
            load_stylesheets()
            // Note: in order to let a plugin provide a custom canvas renderer (the dm3-freifunk-geomap plugin does!)
            // the canvas is created *after* loading the plugins.
            dm3c.canvas = dm3c.trigger_plugin_hook("get_canvas_renderer")[0] || new Canvas()
            // Note: in order to let a plugin provide the initial canvas rendering (the deepamehta3-topicmaps plugin
            // does!) the "init" hook is triggered *after* creating the canvas.
            dm3c.trigger_plugin_hook("init")
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
            // the page panel
            if (dm3c.LOG_GUI) dm3c.log("Setting page panel height: " + $("#canvas").height())
            $("#page-content").height($("#canvas").height())
            //
            $(window).resize(window_resized)
        }

        function load_page_renderers() {

            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + page_renderer_sources.length + " page renderers:")
            for (var i = 0, page_renderer_src; page_renderer_src = page_renderer_sources[i]; i++) {
                load_page_renderer(page_renderer_src)
            }

            function load_page_renderer(page_renderer_src) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + page_renderer_src)
                // load page renderer asynchronously
                dm3c.javascript_source(page_renderer_src, function() {
                    // instantiate
                    var page_renderer_class = js.to_camel_case(js.basename(page_renderer_src))
                    if (LOG_PLUGIN_LOADING) dm3c.log(".......... instantiating \"" + page_renderer_class + "\"")
                    page_renderers[page_renderer_class] = js.new_object(page_renderer_class)
                })
            }
        }

        function load_field_renderers() {
            if (LOG_PLUGIN_LOADING) dm3c.log("Loading " + field_renderer_sources.length + " data field renderers:")
            for (var i = 0, field_renderer_source; field_renderer_source = field_renderer_sources[i]; i++) {
                if (LOG_PLUGIN_LOADING) dm3c.log("..... " + field_renderer_source)
                // load field renderer asynchronously
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
            $("#page-content").height($("#canvas").height())
        }

        function extend_rest_client() {

            dm3c.restc.search_topics_and_create_bucket = function(text, field_uri, whole_word) {
                var params = this.createRequestParameter({search: text, field: field_uri, wholeword: whole_word})
                return this.request("GET", "/webclient/search?" + params.to_query_string())
            }

            // Note: this method is actually part of the Type Search plugin.
            // TODO: proper modularization. Either let the Type Search plugin provide its own REST resource (with
            // another namespace again) or make the Type Search plugin an integral part of the Client plugin.
            dm3c.restc.get_topics_and_create_bucket = function(type_uri) {
                return this.request("GET", "/webclient/search/by_type/" + type_uri)
            }
        }
    })
}
