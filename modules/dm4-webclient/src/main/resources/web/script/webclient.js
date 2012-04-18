var dm4c = new function() {

    // logger preferences
    var ENABLE_LOGGING = false
    var LOG_TYPE_LOADING = false
    this.LOG_PLUGIN_LOADING = false
    var LOG_IMAGE_LOADING = false
    this.LOG_GUI = false
    this.LOG_HISTORY = false

    // preferences
    this.MAX_RESULT_SIZE = 100
    this.DEFAULT_TOPIC_ICON = "/images/ball-gray.png"
    var DEFAULT_FIELD_ROWS = 1

    var CORE_SERVICE_URI = "/core"
    this.COMPOSITE_PATH_SEPARATOR = "/"
    this.REF_PREFIX = "ref_id:"

    // log window
    if (ENABLE_LOGGING) {
        var log_window = window.open()
    }

    // utilities
    this.restc = new RESTClient(CORE_SERVICE_URI)
    this.ui = new GUIToolkit()
    this.render = new RenderHelper()

    // client model
    this.selected_object = null     // a Topic or an Association object, or null if there is no selection

    // view
    this.split_panel = null         // a SplitPanel object
    this.toolbar = null             // the upper toolbar GUI component (a ToolbarPanel object)
    this.canvas = null              // the canvas GUI component that displays the topicmap (a CanvasRenderer object)
    this.page_panel = null          // the page panel GUI component on the right hand side (a PagePanel object)
    this.upload_dialog = null       // the upload dialog (an UploadDialog object)

    var type_cache = new TypeCache()

    var pm = new PluginManager({
        embedded_plugins: [
            "/script/embedded_plugins/default_plugin.js",
            "/script/embedded_plugins/fulltext_plugin.js",
            "/script/embedded_plugins/ckeditor_plugin.js"
        ]
    })

    // ------------------------------------------------------------------------------------------------------ Public API



    // ******************
    // *** Controller ***
    // ******************



    // Note: the controller methods are the top-level entry points to be called by event handlers.
    // The controller methods are responsible for
    //     a) updating the database,
    //     b) updating the client model,
    //     c) updating the view,
    //     d) triggering hooks
    // The names of the controller methods begins with "do_".



    /**
     * Fetches the topic and displays it on the page panel.
     * Triggers the "post_select_topic" hook (indirectly).
     *
     * Precondition: the topic is shown on the canvas.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.do_select_topic = function(topic_id, no_history_update) {
        var topics = dm4c.canvas.select_topic(topic_id)
        // update client model
        set_selected_topic(topics.select, no_history_update)
        // update view
        dm4c.canvas.refresh()
        dm4c.page_panel.display(topics.display)
    }

    /**
     * Fetches the association and displays it on the page panel.
     * Triggers the "post_select_association" hook (indirectly).
     *
     * Precondition: the association is shown on the canvas.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.do_select_association = function(assoc_id, no_history_update) {
        var assoc = dm4c.canvas.select_association(assoc_id)
        // update client model
        set_selected_association(assoc, no_history_update)
        // update view
        dm4c.canvas.refresh()
        dm4c.page_panel.display(assoc)
    }

    /**
     * Triggers the "post_reset_selection" hook (indirectly).
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.do_reset_selection = function(no_history_update) {
        if (dm4c.LOG_HISTORY) dm4c.log("Resetting selection (no_history_update=" + no_history_update + ")")
        // update client model
        reset_selection(no_history_update)
        // update view
        dm4c.canvas.reset_selection(true)    // refresh_canvas=true
        dm4c.page_panel.clear()
        // trigger hook
        var result = dm4c.trigger_plugin_hook("default_page_rendering")
        if (!js.contains(result, false)) {
            dm4c.page_panel.show_splash()
        }
    }

    // ---

    this.do_search = function(searchmode) {
        try {
            var search_topic = build_topic(dm4c.trigger_plugin_hook("search", searchmode)[0])
            // alert("search_topic=" + JSON.stringify(search_topic))
            dm4c.show_topic(search_topic, "show")
        } catch (e) {
            alert("ERROR while searching:\n\n" + JSON.stringify(e))
        }
    }

    // ---

    /**
     * Reveals a topic that is related to the selected topic.
     * Precondition: a topic is selected.
     * <p>
     * Triggers the "pre_show_topic" and "post_show_topic" hooks (indirectly).
     * Triggers the "post_show_association" hook (for each association).
     *
     * @param   topic_id    ID of the related topic.
     */
    this.do_reveal_related_topic = function(topic_id) {
        // fetch from DB
        var assocs = dm4c.restc.get_associations(dm4c.selected_object.id, topic_id)
        // update client model and view
        for (var i = 0, assoc; assoc = assocs[i]; i++) {
            dm4c.show_association(assoc)
        }
        dm4c.show_topic(dm4c.fetch_topic(topic_id), "show", undefined, true)
    }

    // ---

    /**
     * Hides a topic and its visible direct associations from the view (canvas and page panel).
     * Triggers the "post_hide_topic" hook and the "post_hide_association" hook (for each association).
     */
    this.do_hide_topic = function(topic) {
        var assocs = dm4c.canvas.get_associations(topic.id)
        for (var i = 0; i < assocs.length; i++) {
            dm4c.canvas.remove_association(assocs[i].id, false)             // refresh_canvas=false
            dm4c.trigger_plugin_hook("post_hide_association", assocs[i])    // trigger hook
        }
        //
        remove_topic(topic, "post_hide_topic")
    }

    /**
     * Hides an association from the view (canvas and page panel).
     * Triggers the "post_hide_association" hook.
     */
    this.do_hide_association = function(assoc) {
        remove_association(assoc, "post_hide_association")
    }

    // ---

    /**
     * @param   x, y        Optional: the coordinates for placing the topic on the canvas.
     *                      If not specified, placement is up to the canvas.
     */
    this.do_create_topic = function(type_uri, x, y) {
        // update DB
        var topic = dm4c.create_topic(type_uri)
        // update client model and view
        dm4c.show_topic(topic, "edit", {x: x, y: y})
    }

    /**
     * Creates an association between the selected topic and the given topic.
     */
    this.do_create_association = function(type_uri, topic) {
        // update DB
        var assoc = dm4c.create_association(type_uri,
            {topic_id: dm4c.selected_object.id, role_type_uri: "dm4.core.default"},
            {topic_id: topic.id,                role_type_uri: "dm4.core.default"}
        )
        // update client model and view
        dm4c.show_association(assoc, true)                          // refresh_canvas=true
        dm4c.page_panel.display(dm4c.selected_object)
    }

    this.do_create_topic_type = function(topic_type_model) {
        // update DB
        var topic_type = dm4c.create_topic_type(topic_type_model)
        // update client model and view
        dm4c.show_topic(topic_type, "edit")
    }

    // ---

    /**
     * Updates a topic in the DB and on the GUI.
     * Triggers the "post_update_topic" hook (indirectly).
     *
     * @param   old_topic   the topic that is about to be updated. ### FIXME: This "old" version is passed to the
     *                      post_update_topic hook to let plugins compare the old and new ones.
     * @param   new_topic   the new topic that is about to override the old topic.
     *
     * @return  ### FIXDOC: The updated topic as stored in the DB (a Topic object).
     *          Note: the new topic and the updated topic are not necessarily 100% identical. The new topic contains
     *          only the parts that are required for the update, e.g. a composite topic doesn't contain the "value"
     *          field. The updated topic on the other hand is the complete topic as returned by the server.
     */
    this.do_update_topic = function(old_topic, new_topic) {
        // alert("do_update_topic(): new_topic=" + js.stringify(new_topic))
        if (new_topic) {
            // update DB
            var directives = dm4c.restc.update_topic(new_topic)
            // alert("do_update_topic(): directives=" + js.stringify(directives))
            // update client model and view
            process_directives(directives)
            // ### return updated_topic
        } else {
            dm4c.page_panel.refresh()
        }
    }

    /**
     * Updates an association in the DB and on the GUI.
     * Triggers the "post_update_association" hook (indirectly).
     *
     * @param   old_assoc   the association that is about to be updated. ### FIXME: This "old" version is passed to the
     *                      post_update_association hook to let plugins compare the old and new ones.
     * @param   new_assoc   the new association that is about to override the old association.
     *
     * @return  ### FIXME: The updated association as stored in the DB.
     */
    this.do_update_association = function(old_assoc, new_assoc) {
        // update DB
        var directives = dm4c.restc.update_association(new_assoc)
        // update client model and view
        process_directives(directives)
        //
        // ### return updated_assoc
    }

    /**
     * Updates a topic type in the DB and on the GUI.
     * Triggers the "post_update_topic" hook.
     */
    this.do_update_topic_type = function(old_topic_type, new_topic_type) {
        // 1) update DB
        var topic_type = build_topic_type(dm4c.restc.update_topic_type(new_topic_type))
        //
        // 2) update client model (type cache)
        // Note: the type cache must be updated *before* the "post_update_topic" hook is triggered.
        // Other plugins might rely on an up-to-date type cache (e.g. the Type Search plugin does).
        var uri_changed = topic_type.uri != old_topic_type.uri
        if (uri_changed) {
            // alert("Type URI changed: " + old_topic_type.uri + " -> " + topic_type.uri)
            type_cache.remove(old_topic_type.uri)
        }
        type_cache.put_topic_type(topic_type)
        //
        // 3) trigger hook
        dm4c.trigger_plugin_hook("post_update_topic", topic_type, old_topic_type)   // trigger hook
        //
        // 4) update view
        dm4c.canvas.update_topic(topic_type, true)
        dm4c.page_panel.display(topic_type)
        //
        return topic_type
    }

    // ---

    this.do_retype_topic = function(topic, type_uri) {
        // update DB
        var directives = dm4c.restc.update_topic({id: topic.id, type_uri: type_uri})
        // update client model and view
        process_directives(directives)
    }

    // ---

    /**
     * Deletes a topic (including its associations) from the DB and the GUI.
     * Triggers the "post_delete_topic" hook and the "post_delete_association" hook (for each association).
     */
    this.do_delete_topic = function(topic) {
        // update DB
        var directives = dm4c.restc.delete_topic(topic.id)
        // update client model and view
        process_directives(directives)
    }

    /**
     * Deletes an association from the DB and the GUI.
     * Triggers the "post_delete_association" hook.
     */
    this.do_delete_association = function(assoc) {
        // update DB
        var directives = dm4c.restc.delete_association(assoc.id)
        // update client model and view
        process_directives(directives)
    }



    // *************************
    // *** Controller Helper ***
    // *************************



    /**
     * Shows a topic on the canvas, and refreshes the page panel according to the specified action.
     * Triggers the "pre_show_topic" and "post_show_topic" hooks.
     * <p>
     * Preconditions:
     * - the topic exists in the DB.
     * - the topic is (typically) not yet shown on the canvas, and thus:
     *     - the topic is not selected
     *     - the topic has no gemetry yet
     *
     * @param   topic           Topic to add (a Topic object).
     * @param   action          Optional: action to perform, 3 possible values:
     *                              "none" - do not select the topic (page panel doesn't change) -- the default.
     *                              "show" - select the topic and show its info in the page panel.
     *                              "edit" - select the topic and show its form in the page panel.
     * @param   coordinates     Optional: the coordinates for placing the topic on the canvas (an object with
     *                          "x" and "y" properties). If not specified, placement is up to the canvas.
     * @param   do_center       Optional: if evaluates to true the topic is centered on the canvas.
     */
    this.show_topic = function(topic, action, coordinates, do_center) {
        action = action || "none"   // set default
        if (coordinates) {
            topic.x = coordinates.x
            topic.y = coordinates.y
        }
        var do_select = action != "none"
        // Note: the pre_show_topic() hook allows plugins to manipulate the topic, e.g. by setting coordinates
        dm4c.trigger_plugin_hook("pre_show_topic", topic)               // trigger hook
        // update view (canvas)
        var topic_shown = dm4c.canvas.add_topic(topic, do_select)
        if (topic_shown) {
            if (do_center) {
                dm4c.canvas.scroll_topic_to_center(topic_shown.id)
            }
            dm4c.canvas.refresh()
            // update client model
            if (do_select) {
                set_selected_topic(topic_shown)
            }
            //
            dm4c.trigger_plugin_hook("post_show_topic", topic_shown)    // trigger hook
        } else {
            // update client model
            if (do_select) {
                set_selected_topic(topic)
            }
        }
        // update view (page panel)
        update_page_panel()

        function update_page_panel() {
            switch (action) {
            case "none":
                break
            case "show":
                dm4c.page_panel.display(topic)
                break
            case "edit":
                dm4c.begin_editing(topic)
                break
            default:
                throw "WebclientError: \"" + action + "\" is an unexpected page panel action"
            }
        }
    }

    this.show_association = function(assoc, refresh_canvas) {
        // update view (canvas)
        dm4c.canvas.add_association(assoc, refresh_canvas)
        dm4c.trigger_plugin_hook("post_show_association", assoc)    // trigger hook
    }

    // ---

    /**
     * Updates the client model and view according to a set of directives received from server.
     * Precondition: the DB is already up-to-date.
     */
    function process_directives(directives) {
        // alert("process_directives: " + JSON.stringify(directives))
        for (var i = 0, directive; directive = directives[i]; i++) {
            switch (directive.type) {
            case "UPDATE_TOPIC":
                update_topic(build_topic(directive.arg))
                break
            case "DELETE_TOPIC":
                remove_topic(build_topic(directive.arg), "post_delete_topic")
                break
            case "UPDATE_ASSOCIATION":
                update_association(build_association(directive.arg))
                break
            case "DELETE_ASSOCIATION":
                remove_association(build_association(directive.arg), "post_delete_association")
                break
            case "UPDATE_TOPIC_TYPE":
                update_topic_type(build_topic_type(directive.arg))
                break
            default:
                throw "UnknownDirectiveError: directive \"" + directive.type + "\" not implemented"
            }
        }
    }

    // ---

    /**
     * Updates a topic on the view (canvas and page panel).
     * Triggers the "post_update_topic" hook.
     *
     * Called to processes an UPDATE_TOPIC directive.
     *
     * @param   a Topic object
     */
    function update_topic(topic) {
        // update view
        dm4c.canvas.update_topic(topic, true)           // refresh_canvas=true
        dm4c.page_panel.display_conditionally(topic)
        // trigger hook
        dm4c.trigger_plugin_hook("post_update_topic", topic, undefined)         // FIXME: old_topic=undefined
    }

    /**
     * Updates an association on the view (canvas and page panel).
     * Triggers the "post_update_association" hook.
     *
     * Called to processes an UPDATE_ASSOCIATION directive.
     *
     * @param   an Association object
     */
    function update_association(assoc) {
        // update view
        dm4c.canvas.update_association(assoc, true)     // refresh_canvas=true
        dm4c.page_panel.display_conditionally(assoc)
        // trigger hook
        dm4c.trigger_plugin_hook("post_update_association", assoc, undefined)   // FIXME: old_assoc=undefined
    }

    // ---

    /**
     * Removes an topic from the view (canvas and page panel).
     * Triggers the "post_hide_topic" or "post_delete_topic" hook.
     */
    function remove_topic(topic, hook_name) {
        // update view (canvas)
        dm4c.canvas.remove_topic(topic.id, true)            // refresh_canvas=true
        // update client model and view
        reset_selection_conditionally(topic.id)
        // trigger hook
        dm4c.trigger_plugin_hook(hook_name, topic)
    }

    /**
     * Removes an association from the view (canvas and page panel).
     * Triggers "post_hide_association" or "post_delete_association" the hook.
     */
    function remove_association(assoc, hook_name) {
        // update view (canvas)
        dm4c.canvas.remove_association(assoc.id, true)      // refresh_canvas=true
        // update client model and view
        reset_selection_conditionally(assoc.id)
        // trigger hook
        dm4c.trigger_plugin_hook(hook_name, assoc)
    }

    // ---

    function update_topic_type(topic_type) {
        // update client model
        type_cache.put_topic_type(topic_type)
        // update view
        dm4c.refresh_create_menu()
        dm4c.canvas.refresh()
    }

    // ---

    function reset_selection_conditionally(object_id) {
        if (object_id == dm4c.selected_object.id) {
            dm4c.do_reset_selection()
        }
    }



    // ********************
    // *** Client Model ***
    // ********************



    // === Selection ===

    function set_selected_topic(topic, no_history_update) {
        // update client model
        dm4c.selected_object = topic
        //
        if (!no_history_update) {
            push_history(topic)
        }
        // trigger hook
        dm4c.trigger_plugin_hook("post_select_topic", topic)
    }

    function set_selected_association(assoc, no_history_update) {
        // update client model
        dm4c.selected_object = assoc
        // trigger hook
        dm4c.trigger_plugin_hook("post_select_association", assoc)
    }

    function reset_selection(no_history_update) {
        // update client model
        dm4c.selected_object = null
        //
        if (!no_history_update) {
            push_history()
        }
        // trigger hook
        dm4c.trigger_plugin_hook("post_reset_selection")
    }



    // ***********************
    // *** Database Helper ***
    // ***********************



    /**
     * Creates a topic in the DB.
     * Triggers the "post_create_topic" hook.
     *
     * @param   type_uri        The topic type URI, e.g. "dm4.notes.note".
     * @param   composite       Optional.
     *
     * @return  The topic as stored in the DB.
     */
    this.create_topic = function(type_uri, composite) {
        // update DB
        var topic_model = {
            // Note: "uri", "value", and "composite" are optional
            type_uri: type_uri,
            composite: composite    // not serialized to request body if undefined
        }
        var topic = build_topic(dm4c.restc.create_topic(topic_model))
        // trigger hook
        dm4c.trigger_plugin_hook("post_create_topic", topic)
        //
        return topic
    }

    /**
     * Creates an association in the DB.
     *
     * @param   type_uri            The association type URI, e.g. "dm4.core.instantiation".
     * @param   role_1              The topic role or association role at one end (an object).
     *                              Examples for a topic role:
     *                                  {topic_uri: "dm4.core.cardinality", role_type_uri: "dm4.core.type"},
     *                                  {topic_id: 123,                     role_type_uri: "dm4.core.instance"},
     *                              The topic can be identified either by URI or by ID.
     *                              Example for an association role:
     *                                  {assoc_id: 456, role_type_uri: "dm4.core.assoc_def"},
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
        return build_association(dm4c.restc.create_association(assoc_model))
    }

    this.create_topic_type = function(topic_type_model) {
        // 1) update DB
        var topic_type = build_topic_type(dm4c.restc.create_topic_type(topic_type_model))
        // 2) update client model (type cache)
        // Note: the type cache must be updated *before* the "post_create_topic" hook is triggered.
        // Other plugins might rely on an up-to-date type cache (e.g. the Type Search plugin does).
        type_cache.put_topic_type(topic_type)
        // 3) trigger hook
        dm4c.trigger_plugin_hook("post_create_topic", topic_type)
        //
        return topic_type
    }



    // *********************
    // *** Plugin Helper ***
    // *********************



    /**
     * Loads a Javascript file dynamically. Synchronous and asynchronous loading is supported.
     *
     * @param   script_url      The URL (absolute or relative) of the Javascript file to load.
     * @param   callback        Optional: the function to invoke when asynchronous loading is complete.
     *                          If not given loading is performed synchronously.
     */
    this.load_script = function(script_url, callback) {
        $.ajax({
            url: script_url,
            dataType: "script",
            success: callback,
            async: callback != undefined
        })
    }

    this.load_stylesheet = function(css_path) {
        pm.register_css_stylesheet(css_path)
    }

    this.load_page_renderer = function(source_path) {
        pm.register_page_renderer(source_path)
    }

    this.load_field_renderer = function(source_path) {
        pm.register_field_renderer(source_path)
    }

    // ---

    this.register_listener = function(hook_name, listener) {
        pm.register_listener(hook_name, listener)
    }

    // ---

    /**
     * Triggers the registered listeners for the named hook.
     *
     * @param   hook_name   Name of the hook.
     * @param   <varargs>   Variable number of arguments. Passed to the listeners.
     */
    this.trigger_plugin_hook = function(hook_name) {
        return pm.trigger_listeners.apply(undefined, arguments)
    }

    this.trigger_page_renderer_hook = function(topic_or_association, hook_name, args) {
        return dm4c.get_page_renderer(topic_or_association)[hook_name](args)
    }

    this.get_plugin = function(plugin_class) {
        return pm.get_plugin(plugin_class)
    }

    this.get_page_renderer = function(topic_or_association_or_classname) {
        return pm.get_page_renderer(topic_or_association_or_classname)
    }



    // **************
    // *** Helper ***
    // **************



    // === Topics ===

    this.hash_by_id = function(topics) {
        var hashed_topics = {}
        for (var i = 0, topic; topic = topics[i]; i++) {
            hashed_topics[topic.id] = topic
        }
        return hashed_topics
    }

    this.hash_by_type = function(topics) {
        var hashed_topics = {}
        for (var i = 0, topic; topic = topics[i]; i++) {
            hashed_topics[topic.type_uri] = topic
        }
        return hashed_topics
    }

    // ---

    /**
     * Creates an empty topic.
     */
    this.empty_topic = function(topic_type_uri) {
        return {
            id: -1, uri: "", type_uri: topic_type_uri, value: "", composite: {}
        }
    }

    // === Types ===

    /**
     * Looks up a topic type by its uri.
     */
    this.get_topic_type = function(type_uri) {
        return type_cache.get_topic_type(type_uri)
    }

    /**
     * Looks up an association type by its uri.
     */
    this.get_association_type = function(type_uri) {
        return type_cache.get_association_type(type_uri)
    }

    // ---

    /**
     * Convenience method that returns the topic type's label.
     */
    this.type_label = function(type_uri) {
        return dm4c.get_topic_type(type_uri).value
    }

    /**
     * Convenience method that returns the topic type's icon source.
     *
     * @return  The icon source (string).
     */
    this.get_icon_src = function(type_uri) {
        return dm4c.get_topic_type(type_uri).get_icon_src()
    }

    /**
     * Convenience method that returns the topic type's icon.
     *
     * @return  The icon (JavaScript Image object)
     */
    this.get_type_icon = function(topic_type_uri) {
        return dm4c.get_topic_type(topic_type_uri).get_icon()
    }

    /**
     * Convenience method that returns the association type's color.
     *
     * @return  The color (CSS string)
     */
    this.get_type_color = function(assoc_type_uri) {
        return dm4c.get_association_type(assoc_type_uri).get_color()
    }

    // ---

    this.reload_types = function() {
        type_cache.clear()
        load_types()
    }

    // === View Configuration ===

    /**
     * Read out a view configuration setting.
     * <p>
     * Compare to server-side counterparts: WebclientPlugin.getViewConfig() and ViewConfiguration.getSetting()
     *
     * @param   configurable    A topic type, an association type, or an association definition.
     *                          Must not be null/undefined.
     * @param   setting         Last component of the setting URI, e.g. "icon".
     * @paran   lookup_default  Optional: if evaluates to true a default value is looked up in case no setting was made.
     *                          If evaluates to false and no setting was made undefined is returned.
     *
     * @return  The set value, or <code>undefined</code> if no setting was made and lookup_default evaluates to false.
     */
    this.get_view_config = function(configurable, setting, lookup_default) {
        // error check
        if (!configurable.view_config_topics) {
            throw "InvalidConfigurable: no \"view_config_topics\" property found in " + JSON.stringify(configurable)
        }
        // every configurable has an view_config_topics object, however it might be empty
        var view_config = configurable.view_config_topics["dm4.webclient.view_config"]
        if (view_config) {
            var value = view_config.get("dm4.webclient." + setting)
        }
        // lookup default
        if ((value === undefined || value === "") && lookup_default) {
            return dm4c.get_view_config_default(configurable, setting)
        }
        //
        return value
    }

    // Note: for these settings the default is provided by the configurable itself:
    //     "icon"
    //     "color"
    //     "js_page_renderer_class"
    this.get_view_config_default = function(configurable, setting) {
        switch (setting) {
        case "add_to_create_menu":
            return false;
        case "is_searchable_unit":
            return false;
        case "editable":
            return true
        case "viewable":
            return true
        case "js_field_renderer_class":
            return default_field_renderer_class()
        case "rows":
            return DEFAULT_FIELD_ROWS
        default:
            alert("get_view_config_default: setting \"" + setting + "\" not implemented")
        }

        function default_field_renderer_class() {
            switch (configurable.data_type_uri) {
            case "dm4.core.text":
                return "TextFieldRenderer"
            case "dm4.core.html":
                return "HTMLFieldRenderer"
            case "dm4.core.number":
                return "NumberFieldRenderer"
            case "dm4.core.boolean":
                return "BooleanFieldRenderer"
            default:
                alert("get_view_config_default: data type \"" + configurable.data_type_uri +
                    "\" has no default field renderer class")
            }
        }
    }

    // === Commands ===

    this.get_topic_commands = function(topic, context) {
        return get_commands(dm4c.trigger_plugin_hook("topic_commands", topic), context)
    }

    this.get_association_commands = function(assoc, context) {
        return get_commands(dm4c.trigger_plugin_hook("association_commands", assoc), context)
    }

    this.get_canvas_commands = function(cx, cy, context) {
        return get_commands(dm4c.trigger_plugin_hook("canvas_commands", cx, cy), context)
    }

    function get_commands(cmd_lists, context) {
        var commands = []
        for (var i = 0, cmds; cmds = cmd_lists[i]; i++) {
            for (var j = 0, cmd; cmd = cmds[j]; j++) {
                if (matches(cmd, context)) {
                    commands.push(cmd)
                }
            }
        }
        return commands

        function matches(cmd, context) {
            if (typeof cmd.context == "string") {
                return cmd.context == context
            } else {
                return js.contains(cmd.context, context)
            }
        }
    }

    // === Permissions ===

    // ### TODO: handle associations as well
    this.has_write_permission = function(topic) {
        var result = dm4c.trigger_plugin_hook("has_write_permission", topic)
        return !js.contains(result, false)
    }

    // ### TODO: handle association types as well
    this.has_create_permission = function(type_uri) {
        var result = dm4c.trigger_plugin_hook("has_create_permission", dm4c.get_topic_type(type_uri))
        return !js.contains(result, false)
    }

    // === GUI ===

    this.begin_editing = function(object) {
        // update view
        dm4c.page_panel.edit(object)
    }

    // ---

    /**
     * Refreshes a menu so it reflects the current set of known topic types.
     * <p>
     * Utility method for plugin developers.
     *
     * @param   type_menu       a GUIToolkit Menu object.
     * @param   filter_func     Optional: a function that filters the topic types to add to the menu.
     *                          One argument is passed: the topic type (a TopicType object).
     *                          If not specified no filter is applied (all topic types are added).
     */
    this.refresh_type_menu = function(type_menu, filter_func) {
        // save selection
        var item = type_menu.get_selection()
        // remove all items
        type_menu.empty()
        // add topic type items
        type_cache.iterate(function(topic_type) {
            if (!filter_func || filter_func(topic_type)) {
                type_menu.add_item({
                    label: topic_type.value,
                    value: topic_type.uri,
                    icon:  topic_type.get_icon_src()
                })
            }
        })
        // restore selection
        if (item) {
            type_menu.select(item.value)
        }
    }

    /**
     * Utility method for plugin developers.
     */
    this.refresh_create_menu = function() {
        dm4c.refresh_type_menu(dm4c.toolbar.create_menu, function(topic_type) {
            return dm4c.has_create_permission(topic_type.uri) && topic_type.get_menu_config("create-type-menu")
        })
        dm4c.trigger_plugin_hook("post_refresh_create_menu", dm4c.toolbar.create_menu)
    }

    // === Images ===

    var image_tracker   // ### FIXME: the image tracker is global. There can only be one at a time.

    this.create_image = function(src) {
        var img = new Image()
        img.src = src   // Note: if src is a relative URL JavaScript extends img.src to an absolute URL
        img.onload = function() {
            // Note: "this" is the image. The argument is the "load" event.
            if (LOG_IMAGE_LOADING) dm4c.log("Image ready: " + src)
            // notify image tracker
            image_tracker && image_tracker.check()
        }
        return img
    }

    this.create_image_tracker = function(callback_func) {

        return image_tracker = new ImageTracker()

        function ImageTracker() {

            var images = []      // tracked images

            this.add_image = function(image) {
                if (!is_tracked(image)) {
                    images.push(image)
                }
            }

            // Checks if the tracked images are loaded completely.
            // If so, the callback is triggered and this tracker is removed.
            this.check = function() {
                if (is_all_complete()) {
                    callback_func()
                    image_tracker = null
                }
            }

            function is_all_complete() {
                return images.every(function(img) {
                    return img.complete
                })
            }

            function is_tracked(image) {
                return js.includes(images, function(img) {
                    return img.src == image.src
                })
            }
        }
    }

    // === Logging ===

    this.log = function(text) {
        if (ENABLE_LOGGING) {
            // Note: the log window might be closed meanwhile,
            // or it might not apened at all due to browser security restrictions.
            if (log_window && log_window.document) {
                log_window.document.writeln(js.render_text(text) + "<br>")
            }
        }
    }

    // === History ===

    /**
     * Is trueish if the browser supports the HTML5 History API.
     */
    var history_api_supported = window.history.pushState;

    if (this.LOG_HISTORY) this.log("HTML5 History API " + (history_api_supported ? "*is*" : "is *not*") +
        " supported by this browser")

    if (history_api_supported) {
        window.addEventListener("popstate", function(e) {
            // Note: state is null if a) this is the initial popstate event or
            // b) if back is pressed while the begin of history is reached.
            if (e.state) {
                pop_history(e.state)
            } else {
                if (dm4c.LOG_HISTORY) dm4c.log("Popped history state is " + e.state)
            }
        })
    }

    function pop_history(state) {
        if (dm4c.LOG_HISTORY) dm4c.log("Popping history state: " + JSON.stringify(state))
        var result = dm4c.trigger_plugin_hook("pre_pop_history", state)
        if (!js.contains(result, false)) {
            var topic_id = state.topic_id
            dm4c.do_select_topic(topic_id, true)    // no_history_update=true
        } else {
            if (dm4c.LOG_HISTORY) dm4c.log("Generic popping behavoir suppressed by plugin")
        }
    }

    function push_history(topic) {
        if (!history_api_supported) {
            return
        }
        // build history entry
        if (topic) {
            var state = {
                topic_id: topic.id
            }
            var url = "/topic/" + state.topic_id
        } else {
            var state = {}
            var url = ""
        }
        var history_entry = {state: state, url: url}
        // trigger hook
        dm4c.trigger_plugin_hook("pre_push_history", history_entry)
        //
        if (dm4c.LOG_HISTORY) dm4c.log("Pushing history state: " + JSON.stringify(history_entry.state) +
            ", url=\"" + history_entry.url + "\"")
        // push history entry
        history.pushState(history_entry.state, null, history_entry.url)
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    this.fetch_topic = function(topic_id, fetch_composite) {
        return build_topic(dm4c.restc.get_topic_by_id(topic_id, fetch_composite))
    }

    this.fetch_association = function(assoc_id) {
        return build_association(dm4c.restc.get_association(assoc_id))
    }

    function fetch_topic_type(topic_type_uri) {
        // Note: fetch_topic_type() is the only spot where a TopicType object is created directly
        // instead by calling build_topic_type().
        // fetch_topic_type() is part of the Webclient's bootstrapping sequence (see load_types() below).
        return new TopicType(dm4c.restc.get_topic_type(topic_type_uri))
    }

    function fetch_association_type(assoc_type_uri) {
        return build_association_type(dm4c.restc.get_association_type(assoc_type_uri))
    }

    // ---

    function build_topic(topic) {
        return new Topic(topic)
    }

    this.build_topics = function(topics) {
        var topics_array = []
        for (var i = 0, topic; topic = topics[i]; i++) {
            topics_array.push(build_topic(topic))
        }
        return topics_array
    }

    function build_association(assoc) {
        return new Association(assoc)
    }

    function build_topic_type(topic_type) {
        var tt = new TopicType(topic_type)
        // Note: every time a topic type is created its load_icon() method must be called.
        // This can't be done in the TopicType constructor (see load_types() below).
        tt.load_icon()
        return tt
    }

    function build_association_type(assoc_type) {
        return new AssociationType(assoc_type)
    }

    // --- Types ---

    function load_types() {
        // 1) load topic types
        var type_uris = dm4c.restc.get_topic_type_uris()
        if (LOG_TYPE_LOADING) dm4c.log("Loading " + type_uris.length + " topic types")
        for (var i = 0; i < type_uris.length; i++) {
            if (LOG_TYPE_LOADING) dm4c.log("..... " + type_uris[i])
            type_cache.put_topic_type(fetch_topic_type(type_uris[i]))
        }
        // 2) load association types
        var type_uris = dm4c.restc.get_association_type_uris()
        if (LOG_TYPE_LOADING) dm4c.log("Loading " + type_uris.length + " association types")
        for (var i = 0; i < type_uris.length; i++) {
            if (LOG_TYPE_LOADING) dm4c.log("..... " + type_uris[i])
            type_cache.put_association_type(fetch_association_type(type_uris[i]))
        }
        // 3) load topic type icons
        // Note: the icons must be loaded *after* loading the topic types.
        // The topic type "dm4.webclient.icon" must be known.
        if (LOG_TYPE_LOADING) dm4c.log("Loading topic type icons")
        type_cache.iterate(function(topic_type) {
            if (LOG_TYPE_LOADING) dm4c.log("..... " + topic_type.uri)
            topic_type.load_icon()
        })
        if (LOG_TYPE_LOADING) dm4c.log("Loading types complete")
    }

    // ------------------------------------------------------------------------------------------------ Constructor Code

    $(function() {
        //
        // --- 1) Prepare GUI ---
        // create toolbar
        dm4c.toolbar = new ToolbarPanel()
        $("body").append(dm4c.toolbar.dom)
        // create split panel
        dm4c.split_panel = new SplitPanel()
        $("body").append(dm4c.split_panel.dom)
        // create page panel
        dm4c.page_panel = new PagePanel()
        dm4c.split_panel.set_right_panel(dm4c.page_panel)
        // create canvas
        dm4c.canvas = new TopicmapRenderer()
        dm4c.split_panel.set_left_panel(dm4c.canvas)
        // create upload dialog
        dm4c.upload_dialog = new UploadDialog()
        //
        // --- 2) Load Plugins ---
        // Note: in order to let a plugin DOM manipulate the GUI the plugins are loaded *after* the GUI is prepared.
        extend_rest_client()
        load_types()
        //
        pm.load_plugins(setup_gui)

        /**
         * Called once all plugins are loaded.
         */
        function setup_gui() {
            dm4c.log("Setting up GUI")
            // setup create widget
            dm4c.refresh_create_menu()
            if (!dm4c.toolbar.create_menu.get_item_count()) {
                dm4c.toolbar.create_widget.hide()
            }
            // Note: in order to let a plugin provide the initial canvas rendering (the deepamehta-topicmaps plugin
            // does!) the "init" hook is triggered *after* creating the canvas.
            // Note: for displaying an initial topic (the deepamehta-topicmaps plugin does!) the "init" hook must
            // be triggered *after* the GUI setup is complete.
            dm4c.log("Initializing plugins")
            dm4c.trigger_plugin_hook("init")
        }

        function extend_rest_client() {

            dm4c.restc.search_topics_and_create_bucket = function(text, field_uri, whole_word) {
                var params = this.createRequestParameter({search: text, field: field_uri, wholeword: whole_word})
                return this.request("GET", "/webclient/search?" + params.to_query_string())
            }

            // Note: this method is actually part of the Type Search plugin.
            // TODO: proper modularization. Either let the Type Search plugin provide its own REST resource (with
            // another namespace again) or make the Type Search plugin an integral part of the Client plugin.
            dm4c.restc.get_topics_and_create_bucket = function(type_uri, max_result_size) {
                var params = this.createRequestParameter({max_result_size: max_result_size})
                return this.request("GET", "/webclient/search/by_type/" + type_uri + "?" + params.to_query_string())
            }
        }
    })
}
