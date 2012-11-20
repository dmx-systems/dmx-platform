(function() {

function Webclient() {

    // logger preferences
    var ENABLE_LOGGING = false
    //
    this.LOG_TYPE_LOADING = false
    this.LOG_PLUGIN_LOADING = false
    var LOG_IMAGE_LOADING = false
    this.LOG_GUI = false
    this.LOG_HISTORY = false

    // preferences
    this.MAX_RESULT_SIZE = 100
    this.MAX_TOPIC_LINK_CHARS = 50
    this.MAX_TOPIC_LABEL_CHARS = 100
    this.MAX_TOPIC_LABEL_WIDTH = 200    // in pixel
    this.ASSOC_WIDTH = 4
    this.ASSOC_CLICK_TOLERANCE = 0.3
    this.DEFAULT_TOPIC_ICON = "/de.deepamehta.webclient/images/ball-gray.png"
    var DEFAULT_FIELD_ROWS = 1

    var CORE_SERVICE_URI = "/core"
    this.COMPOSITE_PATH_SEPARATOR = "/"
    this.REF_PREFIX = "ref_id:"
    this.DEL_PREFIX = "del_id:"

    // log window
    if (ENABLE_LOGGING) {
        var log_window = window.open()
    }

    // utilities
    this.restc = new RESTClient(CORE_SERVICE_URI)
    this.ui = new GUIToolkit({pre_open_menu: pre_open_menu})
    this.render = new RenderHelper()

    // client model
    this.selected_object = null     // a Topic or an Association object, or null if there is no selection

    // view
    this.split_panel = null         // a SplitPanel object
    this.toolbar = null             // the upper toolbar GUI component (a ToolbarPanel object)
    this.canvas = null              // the canvas GUI component that displays the topicmap (a TopicmapRenderer object)
    this.page_panel = null          // the page panel GUI component on the right hand side (a PagePanel object)

    var type_cache = new TypeCache()

    var pm = new PluginManager({
        internal_plugins: ["default_plugin.js", "fulltext_plugin.js", "ckeditor_plugin.js"]
    })

    extend_rest_client()

    // ------------------------------------------------------------------------------------------------------ Public API



    // ******************
    // *** Controller ***
    // ******************



    // Note: the controller methods consistently update the database and the view.
    // In particular they are responsible for
    //     a) updating the database,
    //     b) updating the client model,
    //     c) updating the view,
    //     d) firing events
    // The names of the controller methods begins with "do_".
    //
    // Your plugin can call the controller methods via the global "dm4c" object.



    /**
     * Fetches the topic and displays it on the page panel.
     * Fires the "post_select_topic" event (indirectly).
     *
     * Precondition: the topic exists in the database AND is shown on the canvas.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.do_select_topic = function(topic_id, no_history_update) {
        dm4c.page_panel.save()
        //
        var topics = dm4c.canvas.select_topic(topic_id)
        // update client model
        set_selected_topic(topics.select, no_history_update)
        // update view
        dm4c.canvas.refresh()
        dm4c.page_panel.render_page(topics.display)
    }

    /**
     * Fetches the association and displays it on the page panel.
     * Fires the "post_select_association" event (indirectly).
     *
     * Precondition: the association exists in the database AND is shown on the canvas.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.do_select_association = function(assoc_id, no_history_update) {
        dm4c.page_panel.save()
        //
        var assoc = dm4c.canvas.select_association(assoc_id)
        // update client model
        set_selected_association(assoc, no_history_update)
        // update view
        dm4c.canvas.refresh()
        dm4c.page_panel.render_page(assoc)
    }

    /**
     * Fires the "post_reset_selection" event (indirectly).
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
        // fire event
        var result = dm4c.fire_event("default_page_rendering")
        if (!js.contains(result, false)) {
            dm4c.page_panel.show_splash()
        }
    }

    // ---

    this.do_search = function(searchmode) {
        dm4c.page_panel.save()
        //
        try {
            var search_topic = build_topic(dm4c.fire_event("search", searchmode)[0])
            dm4c.show_topic(search_topic, "show", undefined, true)      // coordinates=undefined, do_center=true
        } catch (e) {
            alert("ERROR while searching:\n\n" + JSON.stringify(e))
        }
    }

    // ---

    /**
     * Reveals a topic that is related to the selected topic.
     * Precondition: a topic is selected.
     * <p>
     * Fires the "pre_show_topic" and "post_show_topic" events (indirectly).
     * Fires the "post_show_association" event (for each association).
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
        dm4c.show_topic(dm4c.fetch_topic(topic_id), "show", undefined, true)    // coordinates=undefined, do_center=true
    }

    // ---

    /**
     * Hides a topic and its visible direct associations from the view (canvas and page panel).
     * Fires the "post_hide_topic" event and the "post_hide_association" event (for each association).
     */
    this.do_hide_topic = function(topic) {
        var assocs = dm4c.canvas.get_associations(topic.id)
        for (var i = 0; i < assocs.length; i++) {
            dm4c.canvas.remove_association(assocs[i].id, false)     // refresh_canvas=false
            dm4c.fire_event("post_hide_association", assocs[i])     // fire event
        }
        //
        remove_topic(topic, "post_hide_topic")
    }

    /**
     * Hides an association from the view (canvas and page panel).
     * Fires the "post_hide_association" event.
     */
    this.do_hide_association = function(assoc) {
        remove_association(assoc, "post_hide_association")
    }

    // ---

    /**
     * Creates an empty topic in the DB, shows it on the canvas and displays the edit form in the page panel.
     *
     * @param   type_uri    The type of the topic to create.
     * @param   x, y        Optional: the coordinates for placing the topic on the canvas.
     *                      If not specified, placement is up to the canvas.
     */
    this.do_create_topic = function(type_uri, x, y) {
        // update DB
        var topic = dm4c.create_topic(type_uri)
        // update client model and view
        dm4c.show_topic(topic, "edit", {x: x, y: y}, true)      // do_center=true
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
        dm4c.show_association(assoc, "edit")
    }

    this.do_create_topic_type = function(topic_type_model) {
        // update DB
        var topic_type = dm4c.create_topic_type(topic_type_model)
        // update client model and view
        dm4c.show_topic(topic_type, "edit", undefined, true)    // coordinates=undefined, do_center=true
    }

    // ---

    /**
     * Updates a topic in the DB and on the GUI.
     * Fires the "pre_update_topic" and "post_update_topic" (indirectly) event.
     *
     * @param   topic_model     Optional: a topic model containing the data to be udpated.
     *                          If not specified no DB update is performed but the page panel is still refreshed.
     */
    this.do_update_topic = function(topic_model) {
        if (topic_model) {
            dm4c.fire_event("pre_update_topic", topic_model)
            // update DB
            var directives = dm4c.restc.update_topic(topic_model)
            // update GUI (client model and view)
            process_directives(directives)
        } else {
            dm4c.page_panel.refresh()
        }
    }

    /**
     * Updates an association in the DB and on the GUI.
     * Fires the "post_update_association" event (indirectly).
     *
     * @param   assoc_model     an association model containing the data to be udpated.
     */
    this.do_update_association = function(assoc_model, stay_in_edit_mode) {
        // update DB
        var directives = dm4c.restc.update_association(assoc_model)
        // update GUI (client model and view)
        process_directives(directives, stay_in_edit_mode)
    }

    /**
     * Updates a topic type in the DB and on the GUI.
     * Fires the "post_update_topic" event (indirectly).
     *
     * @param   topic_type_model    a topic type model containing the data to be udpated.
     */
    this.do_update_topic_type = function(topic_type_model) {
        // update DB
        var directives = dm4c.restc.update_topic_type(topic_type_model)
        // update GUI (client model and view)
        process_directives(directives)
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
     * Fires the "post_delete_topic" event and the "post_delete_association" event (for each association).
     */
    this.do_delete_topic = function(topic) {
        // update DB
        var directives = dm4c.restc.delete_topic(topic.id)
        // update client model and view
        process_directives(directives)
    }

    /**
     * Deletes an association from the DB and the GUI.
     * Fires the "post_delete_association" event.
     */
    this.do_delete_association = function(assoc) {
        // update DB
        var directives = dm4c.restc.delete_association(assoc.id)
        // alert("do_delete_association(): directives=" + js.stringify(directives))
        // update client model and view
        process_directives(directives)
    }



    // *************************
    // *** Controller Helper ***
    // *************************



    /**
     * Shows a topic on the canvas, and refreshes the page panel according to the specified action.
     * Fires the "pre_show_topic" and "post_show_topic" events.
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
        // Note: the "pre_show_topic" event allows plugins to manipulate the topic, e.g. by setting coordinates
        dm4c.fire_event("pre_show_topic", topic)                // fire event
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
            dm4c.fire_event("post_show_topic", topic_shown)     // fire event
        } else {
            // update client model
            if (do_select) {
                set_selected_topic(topic)
            }
        }
        // update view (page panel)
        update_page_panel(topic, action)
    }

    this.show_association = function(assoc, action) {
        action = action || "none"   // set default
        var do_select = action != "none"
        // update view (canvas)
        dm4c.canvas.add_association(assoc, do_select)
        dm4c.canvas.refresh()
        // update client model
        if (do_select) {
            set_selected_association(assoc)
        }
        //
        dm4c.fire_event("post_show_association", assoc)    // fire event
        // update view (page panel)
        update_page_panel(assoc, action)
    }

    function update_page_panel(topic_or_association, action) {
        switch (action) {
        case "none":
            break
        case "show":
            dm4c.page_panel.render_page(topic_or_association)
            break
        case "edit":
            dm4c.enter_edit_mode(topic_or_association)
            break
        default:
            throw "WebclientError: \"" + action + "\" is an unsupported page panel action"
        }
    }

    // ---

    this.restore_selection = function() {
        if (dm4c.selected_object instanceof Topic) {
            dm4c.do_select_topic(dm4c.selected_object.id)
        } else if (dm4c.selected_object instanceof Association) {
            dm4c.do_select_association(dm4c.selected_object.id)
        }
    }

    // ---

    /**
     * Updates the client model and view according to a set of directives received from server.
     * Precondition: the DB is already up-to-date.
     */
    function process_directives(directives, stay_in_edit_mode) {
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
                update_association(build_association(directive.arg), stay_in_edit_mode)
                break
            case "DELETE_ASSOCIATION":
                remove_association(build_association(directive.arg), "post_delete_association")
                break
            case "UPDATE_TOPIC_TYPE":
                update_topic_type(build_topic_type(directive.arg))
                break
            case "DELETE_TOPIC_TYPE":
                remove_topic_type(directive.arg.uri)
                break
            case "UPDATE_ASSOCIATION_TYPE":
                update_association_type(build_association_type(directive.arg))
                break
            default:
                throw "WebclientError: \"" + directive.type + "\" is an unsupported directive"
            }
        }
    }

    // ---

    /**
     * Updates a topic on the view (canvas and page panel).
     * Fires the "post_update_topic" event.
     *
     * Processes an UPDATE_TOPIC directive.
     *
     * @param   a Topic object
     */
    function update_topic(topic) {
        // update view
        dm4c.canvas.update_topic(topic, true)           // refresh_canvas=true
        dm4c.page_panel.render_page_if_selected(topic)
        // fire event
        dm4c.fire_event("post_update_topic", topic)
    }

    /**
     * Updates an association on the view (canvas and page panel).
     * Fires the "post_update_association" event.
     *
     * Processes an UPDATE_ASSOCIATION directive.
     *
     * @param   an Association object
     */
    function update_association(assoc, stay_in_edit_mode) {
        // update view
        dm4c.canvas.update_association(assoc, true)     // refresh_canvas=true
        stay_in_edit_mode ? dm4c.page_panel.render_form_if_selected(assoc) :
                            dm4c.page_panel.render_page_if_selected(assoc)
        // fire event
        dm4c.fire_event("post_update_association", assoc)
    }

    /**
     * Processes an UPDATE_TOPIC_TYPE directive.
     */
    function update_topic_type(topic_type) {
        // 1) update client model (type cache)
        // Note: the type cache must be updated *before* the "post_update_topic" event is fired.
        // Other plugins might rely on an up-to-date type cache (e.g. the Type Search plugin does).
        type_cache.put_topic_type(topic_type)
        // 2) update view
        // Note: the UPDATE_TOPIC_TYPE directive might result from editing a View Configuration topic.
        // In this case the canvas must be refreshed in order to reflect changed topic icons.
        dm4c.canvas.refresh()
        // 3) fire event
        dm4c.fire_event("post_update_topic", topic_type)
    }

    /**
     * Processes an UPDATE_ASSOCIATION_TYPE directive.
     */
    function update_association_type(assoc_type) {
        // 1) update client model (type cache)
        type_cache.put_association_type(assoc_type)
        // 2) update view
        // Note: the UPDATE_ASSOCIATION_TYPE directive might result from editing a View Configuration topic.
        // In this case the canvas must be refreshed in order to reflect changed association colors.
        dm4c.canvas.refresh()
        // 3) fire event
        // ### dm4c.fire_event("post_update_topic", topic_type)
    }

    // ---

    /**
     * Removes an topic from the view (canvas and page panel).
     * Fires the "post_hide_topic" or "post_delete_topic" event.
     *
     * Processes a DELETE_TOPIC directive.
     */
    function remove_topic(topic, event_name) {
        // update view (canvas)
        dm4c.canvas.remove_topic(topic.id, true)            // refresh_canvas=true
        // update client model and view
        reset_selection_conditionally(topic.id)
        // fire event
        dm4c.fire_event(event_name, topic)
    }

    /**
     * Removes an association from the view (canvas and page panel).
     * Fires "post_hide_association" or "post_delete_association" the event.
     *
     * Processes a DELETE_ASSOCIATION directive.
     */
    function remove_association(assoc, event_name) {
        // update view (canvas)
        dm4c.canvas.remove_association(assoc.id, true)      // refresh_canvas=true
        // update client model and view
        reset_selection_conditionally(assoc.id)
        // fire event
        dm4c.fire_event(event_name, assoc)
    }

    /**
     * Processes a DELETE_TOPIC_TYPE directive.
     */
    function remove_topic_type(type_uri) {
        // update client model (type cache)
        type_cache.remove(type_uri)
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
        // fire event
        dm4c.fire_event("post_select_topic", topic)
    }

    function set_selected_association(assoc, no_history_update) {
        // update client model
        dm4c.selected_object = assoc
        // fire event
        dm4c.fire_event("post_select_association", assoc)
    }

    function reset_selection(no_history_update) {
        // update client model
        dm4c.selected_object = null
        //
        if (!no_history_update) {
            push_history()
        }
        // fire event
        dm4c.fire_event("post_reset_selection")
    }



    // ***********************
    // *** Database Helper ***
    // ***********************



    /**
     * Creates a topic in the DB.
     * Fires the "post_create_topic" event.
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
        // fire event
        dm4c.fire_event("post_create_topic", topic)
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
        // FIXME: no "create" events are fired
        return build_association(dm4c.restc.create_association(assoc_model))
    }

    this.create_topic_type = function(topic_type_model) {
        // 1) update DB
        var topic_type = build_topic_type(dm4c.restc.create_topic_type(topic_type_model))
        // 2) update client model (type cache)
        // Note: the type cache must be updated *before* the "post_create_topic" event is fired.
        // Other plugins might rely on an up-to-date type cache (e.g. the Type Search plugin does).
        type_cache.put_topic_type(topic_type)
        // 3) fire event
        dm4c.fire_event("post_create_topic", topic_type)
        //
        return topic_type
    }



    // *********************
    // *** Plugin Helper ***
    // *********************



    this.add_plugin = function(plugin_uri, plugin_func) {
        pm.add_plugin(plugin_uri, plugin_func)
    }

    this.get_plugin = function(plugin_class) {
        return pm.get_plugin(plugin_class)
    }

    // ---

    this.add_simple_renderer = function(renderer_uri, renderer) {
        pm.add_simple_renderer(renderer_uri, renderer)
    }

    this.get_simple_renderer = function(renderer_uri) {
        return pm.get_simple_renderer(renderer_uri)
    }

    // ---

    this.add_multi_renderer = function(renderer_uri, renderer) {
        pm.add_multi_renderer(renderer_uri, renderer)
    }

    this.get_multi_renderer = function(renderer_uri) {
        return pm.get_multi_renderer(renderer_uri)
    }

    // ---

    this.add_page_renderer = function(renderer_uri, renderer) {
        pm.add_page_renderer(renderer_uri, renderer)
    }

    this.get_page_renderer = function(topic_or_association_or_renderer_uri) {
        return pm.get_page_renderer(topic_or_association_or_renderer_uri)
    }

    // ---

    /**
     * Loads a Javascript file dynamically. Synchronous and asynchronous loading is supported.
     *
     * @param   url     The URL (absolute or relative) of the Javascript file to load.
     * @param   async   Optional (boolean):
     *                      If true loading is asynchronous.
     *                      If false or not given loading is synchronous.
     */
    this.load_script = function(url, async) {
        $.ajax({
            url: url,
            dataType: "script",
            async: async || false,
            error: function(jq_xhr, text_status, error_thrown) {
                throw "WebclientError: loading script " + url + " failed (" + text_status + ": " + error_thrown + ")"
            }
        })
    }

    this.load_stylesheet = function(stylesheet) {
        pm.load_stylesheet(stylesheet)
    }

    // ---

    this.add_listener = function(event_name, listener) {
        pm.add_listener(event_name, listener)
    }

    // ---

    /**
     * Fires an event.
     *
     * @param   event_name  Name of the event.
     * @param   <varargs>   Variable number of event arguments.
     *
     * @return  An array populated with the listener return values. Might be empty.
     *          Note: undefined listener return values are not included in the array, but null values are.
     */
    this.fire_event = function(event_name) {
        return pm.deliver_event.apply(undefined, arguments)
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
        return new Topic({
            id: -1, uri: "", type_uri: topic_type_uri, value: "", composite: {}
        })
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

    this.reload_types = function(callback) {
        // setup tracker
        var tracker = new LoadTracker(2, function() {   // 2 loads: topic types and association types
            adjust_create_widget()
            callback()
        })
        // reload types
        type_cache.clear()
        type_cache.load_types(tracker)
    }

    // === View Configuration ===

    /**
     * Reads out a view configuration setting.
     * <p>
     * Compare to server-side counterparts: WebclientPlugin.getViewConfig() and ViewConfiguration.getSetting()
     *
     * @param   configurable    A topic type or an association type.
     * @param   setting         Last component of the setting URI, e.g. "icon".
     * @param   assoc_def       Optional: if given its setting has precedence.
     *
     * @return  The configuration setting.
     */
    this.get_view_config = function(configurable, setting, assoc_def) {
        // assoc def setting (has precedence)
        if (assoc_def) {
            var value = get_view_config(assoc_def)
            if (is_set(value)) {
                return value
            }
        }
        // type setting
        value = get_view_config(configurable)
        if (is_set(value)) {
            return value
        }
        // default setting
        return get_view_config_default(configurable, setting)

        function is_set(value) {
            // Note 1: we explicitely compare to undefined to let assoc defs override with falsish (0 or false) values.
            //         != is sufficient as these are false: 0 == undefined, false == undefined
            // Note 2: we must regard an empty string as "not set" to get the default renderer URIs.
            //         !== is required as these are true: 0 == "", false == ""
            return value != undefined && value !== ""
        }

        function get_view_config(configurable) {
            // error check
            if (!configurable.view_config_topics) {
                throw "InvalidConfigurableError: no \"view_config_topics\" property found in " +
                    JSON.stringify(configurable)
            }
            // every configurable has an view_config_topics object, however it might be empty
            var view_config = configurable.view_config_topics["dm4.webclient.view_config"]
            if (view_config) {
                return view_config.get("dm4.webclient." + setting)
            }
        }

        function get_view_config_default() {
            switch (setting) {
            case "icon":
                return dm4c.DEFAULT_TOPIC_ICON
            case "color":
                return dm4c.canvas.DEFAULT_ASSOC_COLOR
            case "add_to_create_menu":
                return false;
            case "is_searchable_unit":
                return false;
            case "editable":
                return true
            case "viewable":
                return true
            case "page_renderer_uri":
                return default_page_renderer_uri()
            case "simple_renderer_uri":
                return default_simple_renderer_uri()
            case "multi_renderer_uri":
                return "dm4.webclient.default_multi_renderer"
            case "rows":
                return DEFAULT_FIELD_ROWS
            default:
                throw("WebclientError: \"" + setting + "\" is an unsupported view configuration setting")
            }

            function default_page_renderer_uri() {
                if (configurable instanceof TopicType) {
                    return "dm4.webclient.topic_renderer"
                } else if (configurable instanceof AssociationType) {
                    return "dm4.webclient.association_renderer"
                }
                throw "InvalidConfigurableError: " + JSON.stringify(configurable)
            }

            function default_simple_renderer_uri() {
                switch (configurable.data_type_uri) {
                case "dm4.core.text":
                    return "dm4.webclient.text_renderer"
                case "dm4.core.html":
                    return "dm4.webclient.html_renderer"
                case "dm4.core.number":
                    return "dm4.webclient.number_renderer"
                case "dm4.core.boolean":
                    return "dm4.webclient.boolean_renderer"
                default:
                    throw("WebclientError: \"" + configurable.data_type_uri + "\" is an unsupported data type URI")
                }
            }
        }
    }

    // === Commands ===

    this.get_topic_commands = function(topic, context) {
        return get_commands(dm4c.fire_event("topic_commands", topic), context)
    }

    this.get_association_commands = function(assoc, context) {
        return get_commands(dm4c.fire_event("association_commands", assoc), context)
    }

    this.get_canvas_commands = function(cx, cy, context) {
        return get_commands(dm4c.fire_event("canvas_commands", cx, cy), context)
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

    this.has_write_permission_for_topic = function(topic) {
        var result = dm4c.fire_event("has_write_permission_for_topic", topic)
        return !js.contains(result, false)
    }

    this.has_write_permission_for_association = function(assoc) {
        var result = dm4c.fire_event("has_write_permission_for_association", assoc)
        return !js.contains(result, false)
    }

    // ### TODO: handle association types as well
    this.has_create_permission = function(type_uri) {
        var result = dm4c.fire_event("has_create_permission", dm4c.get_topic_type(type_uri))
        return !js.contains(result, false)
    }

    // === GUI ===

    /**
     * Called once all plugins are loaded.
     * Note: the types are already loaded as well.
     */
    function setup_gui() {
        dm4c.log("Setting up GUI")
        //
        // 1) Setting up the create widget
        // Note: the create menu must be popularized *after* the plugins are loaded.
        // Two events are involved: "post_refresh_create_menu" and "has_create_permission".
        adjust_create_widget()
        //
        // 2) Initialize plugins
        // Note: in order to let a plugin provide the initial canvas rendering (the deepamehta-topicmaps plugin
        // does!) the "init" event is fired *after* creating the canvas.
        // Note: for displaying an initial topic (the deepamehta-topicmaps plugin does!) the "init" event must
        // be fired *after* the GUI setup is complete.
        dm4c.log("Initializing plugins")
        dm4c.fire_event("init")
    }

    function adjust_create_widget() {
        dm4c.refresh_create_menu()
        //
        if (dm4c.toolbar.create_menu.get_item_count()) {
            dm4c.toolbar.create_widget.show()
        } else {
            dm4c.toolbar.create_widget.hide()
        }
    }

    /**
     * Save the page panel before the user opens a menu.
     *
     * @param   menu    a GUIToolkit Menu object.
     */
    function pre_open_menu(menu) {
        // react only on menus that are not part of the page content
        if (menu.dom.parents("#page-content").length == 0) {
            dm4c.page_panel.save()
        }
    }

    /**
     * Save the page panel before the user opens the canvas's context menu.
     */
    this.pre_open_context_menu = function() {
        dm4c.page_panel.save()
    }

    // ---

    // Note: because of the Webclient's intrinsic UI logic -- the page panel displays nothing but the selected
    // object -- no argument should be required here (it should always be dm4c.selected_object). However, custom
    // topicmap renderers may break this principle.
    // In fact the Geomaps renderer does: the selection model contains the sole "Geo Coordinate" topic while
    // the page panel displays the geo-aware topic, e.g. a Person.
    this.enter_edit_mode = function(topic_or_association) {
        // update view
        dm4c.page_panel.render_form(topic_or_association)
    }

    // ---

    /**
     * Refreshes a type menu to reflect the updated type cache (after adding/removing/renaming a type).
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
     * Refreshes the create menu to reflect the updated type cache (after adding/removing/renaming a type).
     * <p>
     * Utility method for plugin developers.
     */
    this.refresh_create_menu = function() {
        dm4c.refresh_type_menu(dm4c.toolbar.create_menu, function(topic_type) {
            return dm4c.has_create_permission(topic_type.uri) && topic_type.get_menu_config("create-type-menu")
        })
        //
        dm4c.fire_event("post_refresh_create_menu", dm4c.toolbar.create_menu)
    }

    // ---

    // ### TODO: formulate this as an jQuery extension
    this.on_return_key = function(element, callback) {
        element.keyup(function(event) {
            if (event.which == 13) {
                callback();
            }
        })
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

    // ### TODO: replace image tracker by load tracker?
    this.create_image_tracker = function(callback) {

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
                    callback()
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

    // === Load Tracker ===

    function LoadTracker(number_of_loads, callback) {
        var loads_tracked = 0
        //
        this.track = function() {
            loads_tracked++
            if (loads_tracked == number_of_loads) {
                callback()
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
        var result = dm4c.fire_event("pre_pop_history", state)
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
        // fire event
        dm4c.fire_event("pre_push_history", history_entry)
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

    this.fetch_association = function(assoc_id, fetch_composite) {
        return build_association(dm4c.restc.get_association_by_id(assoc_id, fetch_composite))
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

    // === REST client ===

    function extend_rest_client() {
        dm4c.restc.search_topics_and_create_bucket = function(text, field_uri) {
            var params = this.createRequestParameter({search: text, field: field_uri})
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

    // ------------------------------------------------------------------------------------------------ Constructor Code

    $(function() {
        // 1) Build GUI
        dm4c.toolbar = new ToolbarPanel()
        $("body").append(dm4c.toolbar.dom)
        //
        dm4c.split_panel = new SplitPanel()
        $("body").append(dm4c.split_panel.dom)
        //
        dm4c.page_panel = new PagePanel()
        dm4c.split_panel.set_right_panel(dm4c.page_panel)
        //
        dm4c.canvas = new DefaultTopicmapRenderer()
        dm4c.split_panel.set_left_panel(dm4c.canvas)
        //
        // 2) Setup Load Tracker
        var items_to_load = pm.retrieve_plugin_list()
        var tracker = new LoadTracker(items_to_load + 2, setup_gui)     // +2 loads: topic types and association types
        //
        // 3) Load Plugins
        pm.load_plugins(tracker)
        //
        // 4) Load Types
        type_cache.load_types(tracker)
    })
}

dm4c = {}   // create global object
Webclient.call(dm4c)
})()
