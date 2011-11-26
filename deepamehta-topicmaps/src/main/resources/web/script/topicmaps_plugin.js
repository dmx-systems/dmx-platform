function topicmaps_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.topicmaps/style/topicmaps.css")
    dm4c.javascript_source("/de.deepamehta.topicmaps/script/topicmap_renderers/canvas_renderer_extension.js")

    var LOG_TOPICMAPS = false

    // Model
    var topicmap_topics             // All topicmaps in the DB (object, key: topicmap ID, value: topicmap topic)
    var topicmaps = {}              // Loaded topicmaps (key: topicmap ID, value: Topicmap object)
    var topicmap                    // Selected topicmap (Topicmap object)
    var topicmap_renderers = {}     // Registered topicmap renderers (key: renderer URI, value: TopicmapRenderer object)
    var topicmap_renderer           // The topicmap renderer of the selected topicmap

    // View
    var topicmap_menu               // A GUIToolkit Menu object

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The selected topicmap
     */
    this.get_topicmap = function() {
        return topicmap
    }



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        fetch_topicmap_topics()
        register_topicmap_renderers()
        extend_rest_client()
        extend_canvas_renderer()
        create_default_topicmap()
        create_topicmap_menu()
        create_topicmap_dialog()
        display_initial_topicmap()

        function extend_rest_client() {
            dm4c.restc.get_topicmap = function(topicmap_id) {
                return this.request("GET", "/topicmap/" + topicmap_id)
            }
            dm4c.restc.add_topic_to_topicmap = function(topicmap_id, topic_id, x, y) {
                return this.request("PUT", "/topicmap/" + topicmap_id + "/topic/" + topic_id + "/" + x + "/" + y)
            }
            dm4c.restc.add_association_to_topicmap = function(topicmap_id, assoc_id) {
                return this.request("PUT", "/topicmap/" + topicmap_id + "/association/" + assoc_id)
            }
            dm4c.restc.remove_association_from_topicmap = function(topicmap_id, assoc_id, ref_id) {
                return this.request("DELETE", "/topicmap/" + topicmap_id + "/association/" + assoc_id + "/" + ref_id)
            }
            dm4c.restc.set_topicmap_translation = function(topicmap_id, trans_x, trans_y) {
                return this.request("PUT", "/topicmap/" + topicmap_id + "/translation/" + trans_x + "/" + trans_y)
            }
        }

        function extend_canvas_renderer() {
            js.extend(dm4c.canvas, CanvasRendererExtension)
        }

        function create_default_topicmap() {
            if (!js.size(topicmap_topics)) {
                create_topicmap_topic("untitled")
                fetch_topicmap_topics()
            }
        }

        function create_topicmap_menu() {
            // build topicmap widget
            var topicmap_label = $("<span>").attr("id", "topicmap-label").text("Topicmap")
            topicmap_menu = dm4c.ui.menu(do_select_topicmap)
            var topicmap_form = $("<div>").attr("id", "topicmap-widget")
                .append(topicmap_label)
                .append(topicmap_menu.dom)
            // put in toolbar
            if ($("#workspace-widget").size()) {
                $("#workspace-widget").after(topicmap_form)
            } else {
                dm4c.toolbar.dom.prepend(topicmap_form)
            }
            //
            rebuild_topicmap_menu(undefined, true)  // no_refetch=true
        }

        function create_topicmap_dialog() {
            var title_input = dm4c.render.input(undefined, 30)
            var type_menu = create_maptype_menu()
            var topicmap_dialog = $("<form>").attr("action", "#").submit(do_create_topicmap)
                .append($("<div>").addClass("field-label").text("Title"))
                .append(title_input.addClass("field-value"))
                .append($("<div>").addClass("field-label").text("Type"))
                .append(type_menu.dom)
            dm4c.ui.dialog("topicmap-dialog", "New Topicmap", topicmap_dialog, "auto", "Create", do_create_topicmap)

            function create_maptype_menu() {
                var menu = dm4c.ui.menu()
                iterate_topicmap_renderers(function(renderer) {
                    var info = renderer.get_info()
                    menu.add_item({label: info.name, value: info.uri})
                })
                return menu
            }

            function do_create_topicmap() {
                $("#topicmap-dialog").dialog("close")
                var name = title_input.val()
                var topicmap_renderer_uri = type_menu.get_selection().value
                create_topicmap(name, topicmap_renderer_uri)
                return false
            }
        }

        function display_initial_topicmap() {
            var groups = location.pathname.match(/\/topicmap\/(\d+)(\/topic\/(\d+))?/)
            if (groups) {
                var topicmap_id = groups[1]
                var topic_id    = groups[3]
                select_menu_item(topicmap_id)
            } else {
                var topicmap_id = get_topicmap_id_from_menu()
            }
            // update model
            set_selected_topicmap(topicmap_id)
            // update view
            display_topicmap()                  // ### FIXME: rethink about history update
            //
            if (topic_id) {
                dm4c.do_select_topic(topic_id)  // ### FIXME: rethink about history update
            }
        }
    }

    this.post_select_topic = function(topic) {
        topicmap.set_topic_selection(topic)
    }

    this.post_select_association = function(assoc) {
        topicmap.set_association_selection(assoc)
    }

    this.post_reset_selection = function() {
        topicmap.reset_selection()
    }

    /**
     * @param   topic   a Topic object
     */
    this.pre_show_topic = function(topic) {
        topicmap.prepare_topic_for_display(topic)
    }

    /**
     * @param   topic   a Topic object with additional "x" and "y" properties
     */
    this.post_show_topic = function(topic) {
        topicmap.add_topic(topic.id, topic.type_uri, topic.value, topic.x, topic.y)
    }

    /**
     * @param   assoc   a CanvasAssoc object
     */
    this.post_show_association = function(assoc) {
        topicmap.add_association(assoc.id, assoc.type_uri, assoc.role_1.topic_id, assoc.role_2.topic_id)
    }

    /**
     * @param   topic   a CanvasTopic object
     */
    this.post_hide_topic = function(topic) {
        topicmap.hide_topic(topic.id)
    }

    /**
     * @param   assoc   a CanvasAssoc object
     */
    this.post_hide_association = function(assoc) {
        topicmap.hide_association(assoc.id)
    }

    /**
     * @param   topic   a CanvasTopic object
     */
    this.post_move_topic = function(topic) {
        topicmap.move_topic(topic.id, topic.x, topic.y)
    }

    /**
     * @param   topic   a Topic object
     */
    this.post_update_topic = function(topic, old_topic) {
        // 1) Update all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Updating topic " + topic.id + " on all topicmaps")
        for (var id in topicmaps) {
            topicmaps[id].update_topic(topic)
        }
        // 2) Update the topicmap menu
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            rebuild_topicmap_menu()
        }
    }

    /**
     * @param   assoc       an Association object
     * @param   old_assoc   FIXME: not yet available
     */
    this.post_update_association = function(assoc, old_assoc) {
        if (LOG_TOPICMAPS) dm4c.log("Updating association " + assoc.id + " on all topicmaps")
        for (var id in topicmaps) {
            topicmaps[id].update_association(assoc)
        }
    }

    /**
     * @param   topic   a Topic object
     */
    this.post_delete_topic = function(topic) {
        // 1) Update all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Deleting topic " + topic.id + " from all topicmaps")
        for (var id in topicmaps) {
            topicmaps[id].delete_topic(topic.id)
        }
        // 2) Update the topicmap menu
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            // remove topicmap model
            delete topicmaps[topic.id]
            //
            var topicmap_id = get_topicmap_id_from_menu()
            if (topicmap_id == topic.id) {
                if (LOG_TOPICMAPS) dm4c.log("..... updating the topicmap menu and selecting the first item " +
                    "(the deleted topic was the CURRENT topicmap)")
                if (!js.size(topicmaps)) {
                    create_topicmap_topic("untitled")
                }
                rebuild_topicmap_menu()
                set_selected_topicmap(get_topicmap_id_from_menu())
                display_topicmap()
            } else {
                if (LOG_TOPICMAPS) dm4c.log("..... updating the topicmap menu and restoring the selection " +
                    "(the deleted topic was ANOTHER topicmap)")
                rebuild_topicmap_menu()
            }
        }
    }

    this.post_delete_association = function(assoc) {
        // Remove association from all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Deleting association " + assoc.id + " from all topicmaps")
        for (var id in topicmaps) {
            topicmaps[id].delete_association(assoc.id)
        }
    }

    this.pre_push_history = function(history_entry) {
        history_entry.state.topicmap_id = topicmap.get_id()
        history_entry.url = "/topicmap/" + topicmap.get_id() + history_entry.url
    }

    this.pre_pop_history = function(state) {
        if (dm4c.LOG_HISTORY) dm4c.log("..... topicmaps_plugin.pre_pop_history()")
        if (state.topicmap_id != topicmap.get_id()) {
            if (dm4c.LOG_HISTORY) dm4c.log(".......... switch from topicmap " + topicmap.get_id() +
                " to " + state.topicmap_id)
            this.do_select_topicmap(state.topicmap_id, true)    // no_history_update=true
            return false
        } else if (!state.topic_id) {
            if (dm4c.LOG_HISTORY) dm4c.log(".......... topicmap not changed and no topic in popstate " +
                "=> resetting selection")
            dm4c.do_reset_selection(true)                       // no_history_update=true
            return false
        }
    }

    this.post_move_canvas = function(trans_x, trans_y) {
        topicmap.set_translation(trans_x, trans_y)
    }

    this.pre_draw_canvas = function(ctx) {
        // Note: topicmap is undefined if canvas draw() is performed
        // before the Topicmaps plugin is initialized.
        topicmap && topicmap.draw_background(ctx)
    }



    // ********************************************************************
    // *** Access Control Hooks (triggered by deepamehta-accesscontrol) ***
    // ********************************************************************



    this.user_logged_in = function(user) {
        rebuild_topicmap_menu()
    }

    this.user_logged_out = function() {
        rebuild_topicmap_menu()
    }



    /******************/
    /*** Controller ***/
    /******************/



    /**
     * Selects a topicmap programmatically.
     * The respective item from the topicmap menu is selected and the topicmap is displayed on the canvas.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.do_select_topicmap = function(topicmap_id, no_history_update) {
        // update model
        set_selected_topicmap(topicmap_id)
        // update view
        select_menu_item(topicmap_id)
        display_topicmap(no_history_update)
    }

    /**
     * Invoked when the user made a selection from the topicmap menu.
     */
    function do_select_topicmap(menu_item) {
        var topicmap_id = menu_item.value
        if (topicmap_id == "_new") {
            open_topicmap_dialog()
        } else {
            // update model
            set_selected_topicmap(topicmap_id)
            // update view
            display_topicmap()
        }
    }

    // ---

    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.canvas_renderer".
     *
     * @return  the topicmap topic.
     */
    this.do_create_topicmap = function(name, topicmap_renderer_uri) {
        return create_topicmap(name, topicmap_renderer_uri)
    }

    // ---

    /**
     * Reloads a topicmap from DB and displays it on the canvas.
     *
     * Prerequisite: the topicmap is already selected in the topicmap menu.
     */
    this.do_refresh_topicmap = function(topicmap_id) {
        // update model
        delete topicmaps[topicmap_id]
        set_selected_topicmap(topicmap_id)
        // update view
        display_topicmap()
    }



    /*************************/
    /*** Controller Helper ***/
    /*************************/



    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.canvas_renderer".
     *
     * @return  the topicmap topic.
     */
    function create_topicmap(name, topicmap_renderer_uri) {
        var topicmap_topic = create_topicmap_topic(name, topicmap_renderer_uri)
        rebuild_topicmap_menu(topicmap_topic.id)
        // update model
        set_selected_topicmap(topicmap_topic.id)
        // update view
        display_topicmap()
        //
        return topicmap_topic
    }



    /*************/
    /*** Model ***/
    /*************/



    function set_selected_topicmap(topicmap_id) {
        if (LOG_TOPICMAPS) dm4c.log("Selecting topicmap " + topicmap_id)
        // update model
        var renderer_uri = topicmap_topics[topicmap_id].get("dm4.topicmaps.topicmap_renderer_uri")
        topicmap_renderer = get_topicmap_renderer(renderer_uri)
        topicmap = load_topicmap(topicmap_id)
    }

    function fetch_topicmap_topics() {
        var topics = dm4c.restc.get_topics("dm4.topicmaps.topicmap", true).items    // fetch_composite=true
        topicmap_topics = dm4c.hash_by_id(dm4c.build_topics(topics))
        // ### FIXME: object properties are not sorted
    }

    /**
     * Loads a topicmap from DB and caches it.
     * If already in cache, the cached topicmap is returned.
     *
     * @return  a Topicmap object
     */
    function load_topicmap(topicmap_id) {
        if (!topicmaps[topicmap_id]) {
            topicmaps[topicmap_id] = topicmap_renderer.load_topicmap(topicmap_id)
        }
        //
        return topicmaps[topicmap_id]
    }

    /**
     * Creates a Topicmap topic in the DB.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.canvas_renderer".
     *
     * @return  The created topic.
     */
    function create_topicmap_topic(name, topicmap_renderer_uri) {
        topicmap_renderer_uri = topicmap_renderer_uri || "dm4.webclient.canvas_renderer"
        //
        if (LOG_TOPICMAPS) dm4c.log("Creating topicmap \"" + name + "\" (topicmap_renderer_uri=\"" +
            topicmap_renderer_uri + "\")")
        //
        var topicmap_state = get_topicmap_renderer(topicmap_renderer_uri).initial_topicmap_state()
        var topicmap = dm4c.create_topic("dm4.topicmaps.topicmap", {
            "dm4.topicmaps.name": name,
            "dm4.topicmaps.topicmap_renderer_uri": topicmap_renderer_uri,
            "dm4.topicmaps.state": topicmap_state
        })
        //
        if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.id)
        //
        return topicmap
    }



    // === Topicmap Renderers ===

    function register_topicmap_renderers() {
        // default renderer
        register(dm4c.canvas)
        // custom renderers
        var renderers = dm4c.trigger_plugin_hook("topicmap_renderer")
        renderers.forEach(function(renderer) {
            register(renderer)
        })

        function register(renderer) {
            topicmap_renderers[renderer.get_info().uri] = renderer
        }
    }

    function get_topicmap_renderer(renderer_uri) {
        var renderer = topicmap_renderers[renderer_uri]
        // error check
        if (!renderer) {
            throw "UnknownTopicmapRendererError: topicmap renderer \"" + renderer_uri + "\" is not registered"
        }
        //
        return renderer
    }

    function iterate_topicmap_renderers(visitor_func) {
        for (var renderer_uri in topicmap_renderers) {
            visitor_func(topicmap_renderers[renderer_uri])
        }
    }

    function switch_topicmap_renderer() {
        var renderer_uri = dm4c.canvas.get_info().uri
        var new_renderer_uri = topicmap.get_renderer_uri()
        if (renderer_uri != new_renderer_uri) {
            if (LOG_TOPICMAPS) dm4c.log("Switching topicmap renderer \"" +
                renderer_uri + "\" => \"" + new_renderer_uri + "\"")
            var renderer = get_topicmap_renderer(new_renderer_uri)
            dm4c.canvas = renderer
            dm4c.split_panel.set_left_panel(renderer)
        }
    }



    /************/
    /*** View ***/
    /************/



    /**
     * Displays the selected topicmap on the canvas.
     *
     * Prerequisite: the topicmap is already selected in the topicmap menu.
     *
     * @param   no_history_update   Optional: boolean.
     */
    function display_topicmap(no_history_update) {
        switch_topicmap_renderer()
        topicmap.put_on_canvas(no_history_update)
    }

    // === Topicmap Menu ===

    /**
     * @param   topicmap_id     Optional: ID of the topicmap to select.
     *                          If not given, the current selection is preserved.
     */
    function rebuild_topicmap_menu(topicmap_id, no_refetch) {
        if (!topicmap_id) {
            topicmap_id = get_topicmap_id_from_menu()
        }
        if (!no_refetch) {
            fetch_topicmap_topics()
        }
        //
        topicmap_menu.empty()
        var icon_src = dm4c.get_icon_src("dm4.topicmaps.topicmap")
        // add topicmaps to menu
        for (var id in topicmap_topics) {
            var topicmap = topicmap_topics[id]
            topicmap_menu.add_item({label: topicmap.value, value: topicmap.id, icon: icon_src})
        }
        // add "New..." to menu
        if (dm4c.has_create_permission("dm4.topicmaps.topicmap")) {
            topicmap_menu.add_separator()
            topicmap_menu.add_item({label: "New Topicmap...", value: "_new", is_trigger: true})
        }
        //
        select_menu_item(topicmap_id)
    }

    /**
     * Selects an item from the topicmap menu.
     */
    function select_menu_item(topicmap_id) {
        topicmap_menu.select(topicmap_id)
    }

    /**
     * Reads out the topicmap menu and returns the topicmap ID.
     * If the topicmap menu has no items yet, undefined is returned.
     */
    function get_topicmap_id_from_menu() {
        var item = topicmap_menu.get_selection()
        if (item) {
            return item.value
        }
    }

    // ---

    function open_topicmap_dialog() {
        $("#topicmap-dialog").dialog("open")
    }
}
