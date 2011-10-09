function topicmaps_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.topicmaps/style/topicmaps.css")

    var LOG_TOPICMAPS = false

    // Model
    var topicmaps = {}              // Topicmaps cache (key: topicmap ID, value: Topicmap object)
    var topicmap                    // Selected topicmap (Topicmap object)
    var topicmap_renderers = {}     // Registered topicmap renderers (key: renderer URI, value: TopicmapRenderer object)

    // View
    var topicmap_menu               // A GUIToolkit Menu object

    // ------------------------------------------------------------------------------------------------ Overriding Hooks



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        var topicmaps = get_all_topicmaps()
        register_topicmap_renderers()
        create_default_topicmap()
        create_topicmap_menu()
        create_topicmap_dialog()
        extend_rest_client()
        select_initial_topicmap()
        display_topicmap()

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
        }

        function create_default_topicmap() {
            if (!topicmaps.length) {
                create_topicmap_topic("untitled")
                topicmaps = get_all_topicmaps()
            }
        }

        function create_topicmap_menu() {
            // build topicmap widget
            var topicmap_label = $("<span>").attr("id", "topicmap-label").text("Topicmap")
            topicmap_menu = dm4c.ui.menu(do_select_topicmap)
            var topicmap_form = $("<div>").attr("id", "topicmap-form")
                .append(topicmap_label)
                .append(topicmap_menu.dom)
            // put in toolbar
            if ($("#workspace-form").size()) {
                $("#workspace-form").after(topicmap_form)
            } else {
                dm4c.toolbar.dom.prepend(topicmap_form)
            }
            //
            rebuild_topicmap_menu(undefined, topicmaps)
        }

        function create_topicmap_dialog() {
            var title_input = dm4c.render.input(undefined, 30)
            var type_menu = create_maptype_menu()
            var topicmap_dialog = $("<form>").attr("action", "#").submit(do_create_topicmap)
                .append($("<div>").addClass("field-label").text("Title"))
                .append(title_input)
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

        function select_initial_topicmap() {
            if (location.search.match(/topicmap=(\d+)/)) {
                var topicmap_id = RegExp.$1
                select_menu_item(topicmap_id)
            } else {
                var topicmap_id = get_topicmap_id_from_menu()
            }
            // update model
            set_selected_topicmap(topicmap_id)
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
     * Restores topic position if topic is already contained in the topicmap but hidden
     *
     * @param   topic   a Topic object
     */
    this.pre_show_topic = function(topic) {
        var t = topicmap.get_topic(topic.id)
        if (t && !t.visibility) {
            topic.x = t.x
            topic.y = t.y
        }
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

    this.pre_draw_canvas = function(ctx) {
        topicmap.draw_background(ctx)
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
     *                                  Default is "dm4.topicmap_renderer.canvas".
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
     *                                  Default is "dm4.topicmap_renderer.canvas".
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



    /**
     * @return  ID of the selected topicmap
     */
    this.get_topicmap_id = function() {
        return topicmap.get_id()
    }

    function set_selected_topicmap(topicmap_id) {
        if (LOG_TOPICMAPS) dm4c.log("Selecting topicmap " + topicmap_id)
        // update model
        topicmap = load_topicmap(topicmap_id)
    }

    function get_all_topicmaps() {
        return dm4c.restc.get_topics("dm4.topicmaps.topicmap", true).items  // sort=true
    }

    /**
     * Loads a topicmap from DB and caches it.
     * If already in cache, the cached topicmap is returned.
     *
     * @return  a Topicmap object
     */
    function load_topicmap(topicmap_id) {
        if (!topicmaps[topicmap_id]) {
            // load from DB
            topicmaps[topicmap_id] = new Topicmap(topicmap_id)
        }
        //
        return topicmaps[topicmap_id]
    }

    /**
     * Creates a Topicmap topic in the DB.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.topicmap_renderer.canvas".
     *
     * @return  The created topic.
     */
    function create_topicmap_topic(name, topicmap_renderer_uri) {
        topicmap_renderer_uri = topicmap_renderer_uri || "dm4.topicmap_renderer.canvas"
        //
        if (LOG_TOPICMAPS) dm4c.log("Creating topicmap \"" + name + "\" (topicmap_renderer_uri=\"" +
            topicmap_renderer_uri + "\")")
        //
        var topicmap = dm4c.create_topic("dm4.topicmaps.topicmap", {
            "dm4.topicmaps.name": name,
            "dm4.topicmaps.topicmap_renderer_uri": topicmap_renderer_uri
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
            throw "UnknownTopicmapRendererException: topicmap renderer \"" + renderer_uri + "\" is not registered"
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

    // ---

    /**
     * @param   topicmap_id     Optional: ID of the topicmap to select.
     *                          If not given, the current selection is preserved.
     */
    function rebuild_topicmap_menu(topicmap_id, topicmaps) {
        if (!topicmap_id) {
            topicmap_id = get_topicmap_id_from_menu()
        }
        if (!topicmaps) {
            topicmaps = get_all_topicmaps()
        }
        //
        topicmap_menu.empty()
        var icon_src = dm4c.get_icon_src("dm4.topicmaps.topicmap")
        // add topicmaps to menu
        for (var i = 0, topicmap; topicmap = topicmaps[i]; i++) {
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



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * An in-memory representation (model) of a persistent topicmap. There are methods for:
     *  - building the in-memory representation by loading a topicmap from DB.
     *  - manipulating the topicmap by e.g. adding/removing topics and associations,
     *    while synchronizing the DB accordingly.
     *  - putting the topicmap on the canvas.
     */
    function Topicmap(topicmap_id) {

        // Model
        var info            // The underlying Topicmap topic (a JavaScript object)
        var topics = {}     // topics of this topicmap (key: topic ID, value: TopicmapTopic object)
        var assocs = {}     // associations of this topicmap (key: association ID, value: TopicmapAssociation object)
        var selected_object_id = -1     // ID of the selected topic or association, or -1 for no selection
        var is_topic_selected           // true indicates topic selection, false indicates association selection
                                        // only evaluated if there is a selection (selected_object_id != -1)
        // View
        var background_image

        load()

        // --- Public API ---

        this.get_id = function() {
            return topicmap_id
        }

        this.get_renderer_uri = function() {
            return info.composite["dm4.topicmaps.topicmap_renderer_uri"]
        }

        /**
         * @param   no_history_update   Optional: boolean.
         */
        this.put_on_canvas = function(no_history_update) {

            track_images()

            /**
             * Defers "put_on_canvas" until all topicmap images are loaded.
             */
            function track_images() {
                var image_tracker = dm4c.create_image_tracker(put_on_canvas)
                // add type icons
                for (var id in topics) {
                    var topic = topics[id]
                    if (topic.visibility) {
                        image_tracker.add_image(dm4c.get_type_icon(topic.type_uri))
                    }
                }
                // add background image
                if (background_image) {
                    image_tracker.add_image(background_image)
                }
                //
                image_tracker.check()
            }

            function put_on_canvas() {

                dm4c.canvas.clear()
                display_topics()
                display_associations()
                dm4c.canvas.refresh()
                //
                restore_selection()

                function display_topics() {
                    for (var id in topics) {
                        var topic = topics[id]
                        if (!topic.visibility) {
                            continue
                        }
                        // Note: canvas.add_topic() expects an topic object with "value" property (not "label")
                        var t = {id: topic.id, type_uri: topic.type_uri, value: topic.label, x: topic.x, y: topic.y}
                        dm4c.canvas.add_topic(t)
                    }
                }

                function display_associations() {
                    for (var id in assocs) {
                        var assoc = assocs[id]
                        var a = {
                            id: assoc.id,
                            type_uri: assoc.type_uri,
                            role_1: {topic_id: assoc.topic_id_1},
                            role_2: {topic_id: assoc.topic_id_2}
                        }
                        dm4c.canvas.add_association(a)
                    }
                }

                function restore_selection() {
                    if (selected_object_id != -1) {
                        if (is_topic_selected) {
                            dm4c.do_select_topic(selected_object_id, no_history_update)
                        } else {
                            dm4c.do_select_association(selected_object_id, no_history_update)
                        }
                    } else {
                        dm4c.do_reset_selection(no_history_update)
                    }
                }
            }
        }

        this.add_topic = function(id, type_uri, label, x, y) {
            var topic = topics[id]
            if (!topic) {
                if (LOG_TOPICMAPS) dm4c.log("Adding topic " + id + " (\"" + label + "\") to topicmap " + topicmap_id)
                // update DB
                var response = dm4c.restc.add_topic_to_topicmap(topicmap_id, id, x, y)
                if (LOG_TOPICMAPS) dm4c.log("..... => ref ID " + response.ref_id)
                // update memory
                topics[id] = new TopicmapTopic(id, type_uri, label, x, y, true, response.ref_id)
            } else if (!topic.visibility) {
                if (LOG_TOPICMAPS)
                    dm4c.log("Showing topic " + id + " (\"" + topic.label + "\") on topicmap " + topicmap_id)
                topic.show()
            } else {
                if (LOG_TOPICMAPS)
                    dm4c.log("Topic " + id + " (\"" + label + "\") already visible in topicmap " + topicmap_id)
            }
        }

        this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
            if (!assocs[id]) {
                if (LOG_TOPICMAPS) dm4c.log("Adding association " + id + " to topicmap " + topicmap_id)
                // update DB
                var response = dm4c.restc.add_association_to_topicmap(topicmap_id, id)
                if (LOG_TOPICMAPS) dm4c.log("..... => ref ID " + response.ref_id)
                // update memory
                assocs[id] = new TopicmapAssociation(id, type_uri, topic_id_1, topic_id_2, response.ref_id)
            } else {
                if (LOG_TOPICMAPS) dm4c.log("Association " + id + " already in topicmap " + topicmap_id)
            }
        }

        this.move_topic = function(id, x, y) {
            var topic = topics[id]
            if (LOG_TOPICMAPS) dm4c.log("Moving topic " + id + " (\"" + topic.label + "\") on topicmap " + topicmap_id
                + " to x=" + x + ", y=" + y)
            topic.move_to(x, y)
        }

        this.hide_topic = function(id) {
            var topic = topics[id]
            if (LOG_TOPICMAPS) dm4c.log("Hiding topic " + id + " (\"" + topic.label + "\") from topicmap " +
                topicmap_id)
            topic.hide()
        }

        this.hide_association = function(id) {
            var assoc = assocs[id]
            if (LOG_TOPICMAPS) dm4c.log("Hiding association " + id + " from topicmap " + topicmap_id)
            assoc.hide()
        }

        /**
         * @param   topic   a Topic object
         */
        this.update_topic = function(topic) {
            var t = topics[topic.id]
            if (t) {
                if (LOG_TOPICMAPS) dm4c.log("..... Updating topic " + t.id + " (\"" + t.label + "\") on topicmap " +
                    topicmap_id)
                t.update(topic)
            }
        }

        /**
         * @param   assoc   an Association object
         */
        this.update_association = function(assoc) {
            var a = assocs[assoc.id]
            if (a) {
                if (LOG_TOPICMAPS) dm4c.log("..... Updating association " + a.id + " on topicmap " + topicmap_id)
                a.update(assoc)
            }
        }

        this.delete_topic = function(id) {
            var topic = topics[id]
            if (topic) {
                if (LOG_TOPICMAPS) dm4c.log("..... Deleting topic " + id + " (\"" + topic.label + "\") from topicmap " +
                    topicmap_id)
                topic.remove()
            }
        }

        this.delete_association = function(id) {
            var assoc = assocs[id]
            if (assoc) {
                if (LOG_TOPICMAPS) dm4c.log("..... Deleting association " + id + " from topicmap " + topicmap_id)
                assoc.remove()
            }
        }

        this.set_topic_selection = function(topic) {
            selected_object_id = topic.id
            is_topic_selected = true
        }

        this.set_association_selection = function(assoc) {
            selected_object_id = assoc.id
            is_topic_selected = false
        }

        this.reset_selection = function() {
            selected_object_id = -1
        }

        this.get_topic = function(id) {
            return topics[id]
        }

        this.draw_background = function(ctx) {
            if (background_image) {
                ctx.drawImage(background_image, 0, 0)
            }
        }

        // --- Private Functions ---

        function load() {

            if (LOG_TOPICMAPS) dm4c.log("Loading topicmap " + topicmap_id)

            var topicmap = dm4c.restc.get_topicmap(topicmap_id)
            info = topicmap.info

            if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.topics.length + " topics")
            load_topics()

            if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.assocs.length + " associations")
            load_associations()

            load_background_image()

            function load_topics() {
                for (var i = 0, topic; topic = topicmap.topics[i]; i++) {
                    var x = topic.visualization["dm4.topicmaps.x"]
                    var y = topic.visualization["dm4.topicmaps.y"]
                    var visibility = topic.visualization["dm4.topicmaps.visibility"]
                    if (LOG_TOPICMAPS) dm4c.log(".......... ID " + topic.id + ": type_uri=\"" + topic.type_uri +
                        "\", label=\"" + topic.value + "\", x=" + x + ", y=" + y + ", visibility=" + visibility +
                        ", ref_id=" + topic.ref_id)
                    topics[topic.id] = new TopicmapTopic(topic.id, topic.type_uri, topic.value, x, y, visibility,
                        topic.ref_id)
                }
            }

            function load_associations() {
                for (var i = 0, assoc; assoc = topicmap.assocs[i]; i++) {
                    if (LOG_TOPICMAPS) dm4c.log(".......... ID " + assoc.id + ": type_uri=\"" + assoc.type_uri +
                            "\", topic_id_1=" + assoc.role_1.topic_id + ", topic_id_2=" + assoc.role_2.topic_id +
                            ", ref_id=" + assoc.ref_id)
                    assocs[assoc.id] = new TopicmapAssociation(assoc.id, assoc.type_uri,
                        assoc.role_1.topic_id, assoc.role_2.topic_id, assoc.ref_id)
                }
            }

            function load_background_image() {
                var file = info.composite["dm4.files.file"]
                if (file) {
                    var image_url = "/proxy/file:" + file["dm4.files.path"]
                    background_image = dm4c.create_image(image_url)
                }
            }
        }

        // --- Private Classes ---

        function TopicmapTopic(id, type_uri, label, x, y, visibility, ref_id) {

            this.id = id
            this.type_uri = type_uri
            this.label = label
            this.x = x
            this.y = y
            this.visibility = visibility
            this.ref_id = ref_id    // ID of the "dm4.topicmaps.topic_mapcontext" association
                                    // that is used by the topicmap to reference this topic.
                                    // ### FIXME: the ref_id should be removed from the client-side model.
                                    // ### TODO: extend topicmaps REST API instead of exposing internals.

            var self = this

            this.show = function() {
                set_visibility(true)
            }

            this.hide = function() {
                set_visibility(false)
                reset_selection()
            }

            this.move_to = function(x, y) {
                // update DB ### TODO: extend topicmaps REST API instead of operating on the DB directly
                dm4c.restc.update_association({id: ref_id, composite: {"dm4.topicmaps.x": x, "dm4.topicmaps.y": y}})
                // update memory
                this.x = x
                this.y = y
            }

            /**
             * @param   topic   a Topic object
             */
            this.update = function(topic) {
                this.label = topic.value
            }

            this.remove = function() {
                // Note: all topic references are deleted already
                delete topics[id]
                reset_selection()
            }

            // ---

            function set_visibility(visibility) {
                // update DB ### TODO: extend topicmaps REST API instead of operating on the DB directly
                dm4c.restc.update_association({id: ref_id, composite: {"dm4.topicmaps.visibility": visibility}})
                // update memory
                self.visibility = visibility
            }

            function reset_selection() {
                if (is_topic_selected && selected_object_id == id) {
                    selected_object_id = -1
                }
            }
        }

        function TopicmapAssociation(id, type_uri, topic_id_1, topic_id_2, ref_id) {

            this.id = id
            this.type_uri = type_uri
            this.topic_id_1 = topic_id_1
            this.topic_id_2 = topic_id_2
            this.ref_id = ref_id    // ID of the "dm4.topicmaps.association_mapcontext" association
                                    // that is used by the topicmap to reference this association.
                                    // ### FIXME: the ref_id should be removed from the client-side model.
                                    // ### TODO: extend topicmaps REST API instead of exposing internals.

            this.hide = function() {
                // update DB
                dm4c.restc.remove_association_from_topicmap(topicmap_id, id, ref_id)
                // update memory
                delete assocs[id]
                reset_selection()
            }

            /**
             * @param   assoc   an Association object
             */
            this.update = function(assoc) {
                this.type_uri = assoc.type_uri
            }

            this.remove = function() {
                delete assocs[id]
                reset_selection()
            }

            // ---

            function reset_selection() {
                if (!is_topic_selected && selected_object_id == id) {
                    selected_object_id = -1
                }
            }
        }
    }
}
