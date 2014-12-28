dm4c.add_plugin("de.deepamehta.topicmaps", function() {

    var self = this

    dm4c.load_script("/de.deepamehta.topicmaps/script/topicmap_viewmodel.js")

    // Model
    var topicmap                    // Selected topicmap (TopicmapViewmodel object)     \ updated together by
    var topicmap_renderer           // The topicmap renderer of the selected topicmap   / set_selected_topicmap()
    var topicmap_renderers = {}     // Registered topicmap renderers (key: renderer URI, value: TopicmapRenderer object)
    var topicmap_topics = {}        // Loaded topicmap topics, grouped by workspace, an object:
                                    //   {
                                    //     workspaceId: [topicmapTopic]
                                    //   }
    var selected_topicmap_ids = {}  // ID of the selected topicmap, per-workspace, an object:
                                    //   {
                                    //     workspaceId: selectedTopicmapId
                                    //   }
    var topicmap_cache = {}         // Loaded topicmaps (key: topicmap ID, value: TopicmapViewmodel object)

    // View
    var topicmap_menu               // A GUIToolkit Menu object



    // === REST Client Extension ===

    dm4c.restc.create_topicmap = function(name, topicmap_renderer_uri) {
        return this.request("POST", "/topicmap/" + encodeURIComponent(name) + "/" + topicmap_renderer_uri)
    }

    /**
     * @param   include_childs (boolean)    Optional: if true the topics contained in the returned topicmap will
     *                                      include their child topics. Default is false.
     */
    dm4c.restc.get_topicmap = function(topicmap_id, include_childs) {
        var params = this.createRequestParameter({include_childs: include_childs})
        return this.request("GET", "/topicmap/" + topicmap_id + params.to_query_string())
    }
    dm4c.restc.add_topic_to_topicmap = function(topicmap_id, topic_id, view_props) {
        this.request("POST", "/topicmap/" + topicmap_id + "/topic/" + topic_id, view_props)
    }
    dm4c.restc.add_association_to_topicmap = function(topicmap_id, assoc_id) {
        this.request("POST", "/topicmap/" + topicmap_id + "/association/" + assoc_id)
    }
    dm4c.restc.set_view_properties = function(topicmap_id, topic_id, view_props) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/topic/" + topic_id, view_props)
    }
    dm4c.restc.set_topic_position = function(topicmap_id, topic_id, x, y) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/topic/" + topic_id + "/" + x + "/" + y)
    }
    dm4c.restc.set_topic_visibility = function(topicmap_id, topic_id, visibility) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/topic/" + topic_id + "/" + visibility)
    }
    dm4c.restc.remove_association_from_topicmap = function(topicmap_id, assoc_id) {
        this.request("DELETE", "/topicmap/" + topicmap_id + "/association/" + assoc_id)
    }
    dm4c.restc.set_cluster_position = function(topicmap_id, cluster_coords) {
        this.request("PUT", "/topicmap/" + topicmap_id, cluster_coords)
    }
    dm4c.restc.set_topicmap_translation = function(topicmap_id, trans_x, trans_y) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/translation/" + trans_x + "/" + trans_y)
    }



    // === Webclient Listeners ===

    /**
     * Note: plugins are supposed to register their view customizers and viewmodel customizers at init_2.
     * Registering the topicmap renderers at init(1) ensures they are available for being customized.
     */
    dm4c.add_listener("init", function() {

        register_topicmap_renderers()

        function register_topicmap_renderers() {
            // default renderer
            register(dm4c.topicmap_renderer)
            // custom renderers
            var renderers = dm4c.fire_event("topicmap_renderer")
            renderers.forEach(function(renderer) {
                register(renderer)
            })

            function register(renderer) {
                topicmap_renderers[renderer.get_info().uri] = renderer
            }
        }
    })

    /**
     * Note: the Workspaces plugin initializes at init(1).
     * Initializing the topicmaps model at init_2 ensures a selected workspace is already known.
     */
    dm4c.add_listener("init_2", function() {

        create_topicmap_menu()
        init_model()    // Note: the topicmap menu must already be created

        function create_topicmap_menu() {
            // build topicmap widget
            var topicmap_label = $("<span>").attr("id", "topicmap-label").text("Topicmap")
            topicmap_menu = dm4c.ui.menu(do_select_topicmap)
            var topicmap_widget = $("<div>").attr("id", "topicmap-widget")
                .append(topicmap_label)
                .append(topicmap_menu.dom)
            // put in toolbar
            if ($("#workspace-widget").length) {
                $("#workspace-widget").after(topicmap_widget)
            } else {
                dm4c.toolbar.dom.prepend(topicmap_widget)
            }

            function do_select_topicmap(menu_item) {
                var topicmap_id = menu_item.value
                if (topicmap_id == "_new") {
                    do_open_topicmap_dialog()
                } else {
                    select_topicmap(topicmap_id)
                }
            }
        }

        function do_open_topicmap_dialog() {
            var title_input = dm4c.render.input(undefined, 30)
            var type_menu = create_maptype_menu()
            var dialog_content = dm4c.render.label("Title").add(title_input)
            if (type_menu.get_item_count() > 1) {
                dialog_content = dialog_content.add(dm4c.render.label("Type")).add(type_menu.dom)
            }
            dm4c.ui.dialog({
                title: "New Topicmap",
                content: dialog_content,
                button_label: "Create",
                button_handler: do_create_topicmap
            })

            function create_maptype_menu() {
                var menu = dm4c.ui.menu()
                iterate_topicmap_renderers(function(renderer) {
                    var info = renderer.get_info()
                    menu.add_item({label: info.name, value: info.uri})
                })
                return menu
            }

            function do_create_topicmap() {
                var name = title_input.val()
                var topicmap_renderer_uri = type_menu.get_selection().value
                create_topicmap(name, topicmap_renderer_uri)
            }
        }
    })

    /**
     * Displays the initial topicmap.
     *
     * Note: plugins are supposed to register their view customizers and viewmodel customizers at init_2.
     * Displaying the initial topicmap at init_3 ensures all customizers are registered already.
     */
    dm4c.add_listener("init_3", function() {
        display_topicmap()                  // ### FIXME: rethink about history update
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        // update the topicmap menu
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            fetch_topicmap_topics_and_refresh_menu()
        }
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_delete_topic", function(topic) {
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            // 1) update model
            invalidate_topicmap_cache(topic.id)
            //
            // 2) update model + view
            fetch_topicmap_topics_and_refresh_menu()
            // If the deleted topicmap was the CURRENT topicmap we must select another one from the topicmap menu.
            // Note: if the last topicmap was deleted another one is already created.
            if (topic.id == topicmap.get_id()) {
                select_topicmap(get_topicmap_id_from_menu())
            }
        }
    })

    dm4c.add_listener("pre_push_history", function(history_entry) {
        history_entry.state.topicmap_id = topicmap.get_id()
        history_entry.url = "/topicmap/" + topicmap.get_id() + history_entry.url
    })

    dm4c.add_listener("pre_pop_history", function(state) {
        if (state.topicmap_id != topicmap.get_id()) {
            // switch topicmap
            self.select_topicmap(state.topicmap_id, true)       // no_history_update=true
            return false
        } else if (!state.topic_id) {
            // topicmap not changed and no topic in popstate
            dm4c.do_reset_selection(true)                       // no_history_update=true
            return false
        }
    })

    dm4c.add_listener("pre_draw_canvas", function(ctx) {
        // Note: topicmap is undefined if canvas draw() is performed
        // before the Topicmaps plugin is initialized.
        topicmap && topicmap.draw_background(ctx)
    })



    // === Workspace Listeners ===

    dm4c.add_listener("post_select_workspace", function(workspace_id) {
        fetch_topicmap_topics_and_refresh_menu()    // update model + view
        // Note: the topicmap permissions are refreshed in the course of refetching the topicmap topics.
        //
        var topicmap_id = selected_topicmap_ids[workspace_id]
        if (!topicmap_id) {
            topicmap_id = get_topicmap_id_from_menu()
        }
        //
        self.select_topicmap(topicmap_id)           // update model + view
    })



    // === Access Control Listeners ===

    dm4c.add_listener("logged_in", function(username) {
        fetch_topicmap_topics_and_refresh_menu()
        // Note: the topicmap permissions are refreshed in the course of refetching the topicmap topics.
        //
        clear_topicmap_cache()
        reload_topicmap()
    })

    dm4c.add_listener("logged_out", function() {
        clear_topicmap_cache()
        // Note: the topicmap is switched/reloaded by the "post_select_workspace" listener (above)
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The selected topicmap
     */
    this.get_topicmap = function() {
        return topicmap
    }

    this.iterate_topicmaps = function(visitor_func) {
        for (var topicmap_id in topicmap_cache) {
            visitor_func(topicmap_cache[topicmap_id])
        }
    }

    this.get_topicmap_renderer = function(renderer_uri) {
        return get_topicmap_renderer(renderer_uri)
    }

    /**
     * Selects a topicmap programmatically.
     * The respective item from the topicmap menu is selected and the topicmap is displayed.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.select_topicmap = function(topicmap_id, no_history_update) {
        select_menu_item(topicmap_id)
        select_topicmap(topicmap_id, no_history_update)
    }

    // ---

    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  the topicmap topic.
     */
    this.create_topicmap = function(name, topicmap_renderer_uri) {
        return create_topicmap(name, topicmap_renderer_uri)
    }

    /**
     * Puts a new topicmap in the topicmap menu, selects and displays it.
     * Plugins call this method when they have created a topicmap (at server-side) and now want display it.
     */
    this.add_topicmap = function(topicmap_id) {
        add_topicmap(topicmap_id)
    }
    
    // ----------------------------------------------------------------------------------------------- Private Functions



    // *************************
    // *** Controller Helper ***
    // *************************



    /**
     * Updates the model to reflect the given topicmap is now selected, and displays it.
     *
     * Prerequisite: the topicmap menu already shows the selected topicmap.
     *
     * @param   no_history_update   Optional: boolean.
     */
    function select_topicmap(topicmap_id, no_history_update) {
        // update model
        set_selected_topicmap(topicmap_id)
        // update view
        display_topicmap(no_history_update)
    }

    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays it.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  the topicmap topic.
     */
    function create_topicmap(name, topicmap_renderer_uri) {
        // update DB
        var topicmap_topic = create_topicmap_topic(name, topicmap_renderer_uri)
        // update model + view
        add_topicmap(topicmap_topic.id)
        //
        return topicmap_topic
    }

    /**
     * Creates an empty topicmap (a topic of type "Topicmap") in the DB.
     *
     * @param   name                    The name of the topicmap
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  The created Topicmap topic.
     */
    function create_topicmap_topic(name, topicmap_renderer_uri) {
        return dm4c.restc.create_topicmap(name, topicmap_renderer_uri || "dm4.webclient.default_topicmap_renderer")
    }

    /**
     * Puts a new topicmap in the topicmap menu, selects and displays it.
     * This is called when a new topicmap is created at server-side and now should be displayed.
     */
    function add_topicmap(topicmap_id) {
        fetch_topicmap_topics_and_refresh_menu()    // update model + view
        self.select_topicmap(topicmap_id)           // update model + view
    }

    /**
     * Reloads the current topicmap from DB and displays it.
     */
    function reload_topicmap() {
        // 1) update model
        // Note: the cookie and the renderer are already up-to-date
        var topicmap_id = topicmap.get_id()
        invalidate_topicmap_cache(topicmap_id)
        topicmap = get_topicmap(topicmap_id)
        // 2) update view
        display_topicmap()
    }

    function fetch_topicmap_topics_and_refresh_menu() {
        // 1) update model
        var count = fetch_topicmap_topics()
        // create default topicmap
        if (!count) {
            create_topicmap_topic("untitled")
            fetch_topicmap_topics()
        }
        // 2) update view
        refresh_topicmap_menu()
    }



    // *************
    // *** Model ***
    // *************



    function init_model() {
        fetch_topicmap_topics_and_refresh_menu()
        //
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
        if (topic_id) {
            topicmap.set_topic_selection(topic_id)
        }
    }

    /**
     * Updates the model to reflect the given topicmap is now selected. That includes setting a cookie
     * and updating 3 model objects ("topicmap", "topicmap_renderer", "selected_topicmap_ids").
     *
     * Prerequisite: the topicmap topic for the specified topicmap is already loaded/up-to-date.
     */
    function set_selected_topicmap(topicmap_id) {
        // 1) set cookie
        // Note: the cookie must be set *before* the topicmap is loaded.
        // Server-side topic loading might depend on the topicmap type.
        js.set_cookie("dm4_topicmap_id", topicmap_id)
        //
        // 2) update "topicmap_renderer"
        // Note: the renderer must be set *before* the topicmap is loaded.
        // The renderer is responsible for loading.
        var renderer_uri = get_topicmap_topic(topicmap_id).get("dm4.topicmaps.topicmap_renderer_uri")
        topicmap_renderer = get_topicmap_renderer(renderer_uri)
        //
        // 3) update "topicmap" and "selected_topicmap_ids"
        topicmap = get_topicmap(topicmap_id)
        selected_topicmap_ids[get_workspace_id()] = topicmap_id
    }

    /**
     * Looks up a topicmap topic from the model and returns it.
     *
     * Prerequisite: the specified topicmap must be assigned to the currently selected workspace.
     */
    function get_topicmap_topic(topicmap_id) {
        var workspace_id = get_workspace_id()
        var topicmap_topic = find_topic(get_topicmap_topics(workspace_id), topicmap_id)
        if (!topicmap_topic) {
            throw "TopicmapsError: topicmap " + topicmap_id + " not found in model for workspace " + workspace_id
        }
        return topicmap_topic
    }

    function get_topicmap_topics(workspace_id) {
        return topicmap_topics[workspace_id]
    }

    /**
     * Returns a topicmap from the cache.
     * If not in cache, the topicmap is loaded and cached before.
     */
    function get_topicmap(topicmap_id) {
        var topicmap = topicmap_cache[topicmap_id]
        if (!topicmap) {
            topicmap = load_topicmap(topicmap_id)
        }
        return topicmap
    }

    /**
     * Loads a topicmap from DB and caches it.
     *
     * Prerequisite: the topicmap renderer responsible for loading is already set.
     *
     * @return  the loaded topicmap (a TopicmapViewmodel).
     */
    function load_topicmap(topicmap_id) {
        var config = {
            is_writable: dm4c.has_write_permission_for_topic(get_topicmap_topic(topicmap_id))
        }
        var topicmap = topicmap_renderer.load_topicmap(topicmap_id, config)
        put_in_cache(topicmap)
        //
        return topicmap
    }

    // ---

    function fetch_topicmap_topics() {
        var workspace_id = get_workspace_id()
        var topics = dm4c.restc.get_assigned_topics(workspace_id, "dm4.topicmaps.topicmap", true) // include_childs=true
        topicmap_topics[workspace_id] = dm4c.build_topics(topics)
        // ### TODO: sort topicmaps by name
        return topics.length
    }

    function get_workspace_id() {
        return dm4c.get_plugin("de.deepamehta.workspaces").get_workspace_id()
    }



    // === Topicmap Renderers ===

    function get_topicmap_renderer(renderer_uri) {
        var renderer = topicmap_renderers[renderer_uri]
        // error check
        if (!renderer) {
            throw "TopicmapsError: \"" + renderer_uri + "\" is an unknown topicmap renderer"
        }
        //
        return renderer
    }

    function iterate_topicmap_renderers(visitor_func) {
        for (var renderer_uri in topicmap_renderers) {
            visitor_func(topicmap_renderers[renderer_uri])
        }
    }



    // === Helper ===

    function find_topic(topics, id) {
        return js.find(topics, function(topic) {
            return topic.id == id
        })
    }



    // === Topicmap Cache ===

    function put_in_cache(topicmap) {
        topicmap_cache[topicmap.get_id()] = topicmap
    }

    function clear_topicmap_cache() {
        topicmap_cache = {}
    }

    function invalidate_topicmap_cache(topicmap_id) {
        delete topicmap_cache[topicmap_id]
    }



    // ************
    // *** View ***
    // ************



    /**
     * Displays the selected topicmap based on the model ("topicmap", "topicmap_renderer").
     *
     * Prerequisite: the topicmap menu already shows the selected topicmap.
     *
     * @param   no_history_update   Optional: boolean.
     */
    function display_topicmap(no_history_update) {
        switch_topicmap_renderer()
        topicmap_renderer.display_topicmap(topicmap, no_history_update)
    }

    function switch_topicmap_renderer() {
        var renderer_uri = dm4c.topicmap_renderer.get_info().uri
        var new_renderer_uri = topicmap_renderer.get_info().uri
        if (renderer_uri != new_renderer_uri) {
            // switch topicmap renderer
            dm4c.topicmap_renderer = topicmap_renderer
            dm4c.split_panel.set_topicmap_renderer(topicmap_renderer)
        }
    }

    // === Topicmap Menu ===

    /**
     * Refreshes the topicmap menu based on the model ("topicmap_topics").
     */
    function refresh_topicmap_menu() {
        var icon_src = dm4c.get_type_icon_src("dm4.topicmaps.topicmap")
        var topicmap_id = get_topicmap_id_from_menu()   // save selection
        topicmap_menu.empty()
        // add topicmaps to menu
        var topicmap_topics = get_topicmap_topics(get_workspace_id())
        for (var i = 0, topicmap_topic; topicmap_topic = topicmap_topics[i]; i++) {
            topicmap_menu.add_item({label: topicmap_topic.value, value: topicmap_topic.id, icon: icon_src})
        }
        // add "New..." to menu
        if (dm4c.has_create_permission("dm4.topicmaps.topicmap")) {
            topicmap_menu.add_separator()
            topicmap_menu.add_item({label: "New Topicmap...", value: "_new", is_trigger: true})
        }
        // restore selection
        select_menu_item(topicmap_id)
        //
        dm4c.fire_event("post_refresh_topicmap_menu", topicmap_menu)
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
        return item && item.value
    }
})
