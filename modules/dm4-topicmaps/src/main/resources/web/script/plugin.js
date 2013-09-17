dm4c.add_plugin("de.deepamehta.topicmaps", function() {

    var LOG_TOPICMAPS = false
    var self = this

    dm4c.load_script("/de.deepamehta.topicmaps/script/topicmap_viewmodel.js")

    // Model
    var topicmap                    // Selected topicmap (TopicmapViewmodel object)     \ updated together by
    var topicmap_renderer           // The topicmap renderer of the selected topicmap   / set_selected_topicmap()
    var topicmap_renderers = {}     // Registered topicmap renderers (key: renderer URI, value: TopicmapRenderer object)
    var topicmap_topics             // All topicmaps in the DB (object, key: topicmap ID, value: topicmap topic)
    var topicmap_cache = {}         // Loaded topicmaps (key: topicmap ID, value: TopicmapViewmodel object)

    // View
    var topicmap_menu               // A GUIToolkit Menu object



    // === REST Client Extension ===

    dm4c.restc.get_topicmap = function(topicmap_id) {
        return this.request("GET", "/topicmap/" + topicmap_id)
    }
    dm4c.restc.create_topicmap = function(name, topicmap_renderer_uri) {
        return this.request("POST", "/topicmap/" + encodeURIComponent(name) + "/" + topicmap_renderer_uri)
    }
    dm4c.restc.add_topic_to_topicmap = function(topicmap_id, topic_id, x, y) {
        this.request("POST", "/topicmap/" + topicmap_id + "/topic/" + topic_id + "/" + x + "/" + y)
    }
    dm4c.restc.add_association_to_topicmap = function(topicmap_id, assoc_id) {
        this.request("POST", "/topicmap/" + topicmap_id + "/association/" + assoc_id)
    }
    dm4c.restc.move_topic = function(topicmap_id, topic_id, x, y) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/topic/" + topic_id + "/" + x + "/" + y)
    }
    dm4c.restc.set_topic_visibility = function(topicmap_id, topic_id, visibility) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/topic/" + topic_id + "/" + visibility)
    }
    dm4c.restc.remove_association_from_topicmap = function(topicmap_id, assoc_id) {
        this.request("DELETE", "/topicmap/" + topicmap_id + "/association/" + assoc_id)
    }
    dm4c.restc.move_cluster = function(topicmap_id, cluster_coords) {
        this.request("PUT", "/topicmap/" + topicmap_id, cluster_coords)
    }
    dm4c.restc.set_topicmap_translation = function(topicmap_id, trans_x, trans_y) {
        this.request("PUT", "/topicmap/" + topicmap_id + "/translation/" + trans_x + "/" + trans_y)
    }



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {

        var topicmap_dialog

        register_topicmap_renderers()
        create_topicmap_menu()
        create_topicmap_dialog()
        display_initial_topicmap()

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
            //
            refresh_topicmap_menu()

            function do_select_topicmap(menu_item) {
                var topicmap_id = menu_item.value
                if (topicmap_id == "_new") {
                    topicmap_dialog.open()
                } else {
                    // update model
                    set_selected_topicmap(topicmap_id)
                    // update view
                    display_topicmap()
                }
            }
        }

        function create_topicmap_dialog() {
            var title_input = dm4c.render.input(undefined, 30)
            var type_menu = create_maptype_menu()
            var dialog_content = $("<form>").attr("action", "#").submit(do_create_topicmap)
                .append($("<div>").addClass("field-label").text("Title"))
                .append(title_input)
            if (type_menu.get_item_count() > 1) {
                dialog_content
                    .append($("<div>").addClass("field-label").text("Type"))
                    .append(type_menu.dom)
            }
            topicmap_dialog = dm4c.ui.dialog({
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
                topicmap_dialog.close()
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
    })

    /* ### dm4c.add_listener("post_select_topic", function(topic) {
        topicmap.set_topic_selection(topic)
    }) */

    /* ### dm4c.add_listener("post_select_association", function(assoc) {
        topicmap.set_association_selection(assoc)
    }) */

    dm4c.add_listener("post_reset_selection", function() {
        topicmap.reset_selection()
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("pre_show_topic", function(topic) {
        topicmap.prepare_topic_for_display(topic)
    })

    /**
     * @param   topic   a Topic object with additional "x" and "y" properties
     */
    /* ### dm4c.add_listener("post_show_topic", function(topic) {
        topicmap.add_topic(topic.id, topic.type_uri, topic.value, topic.x, topic.y)
    }) */

    /**
     * @param   assoc   a AssociationView object
     */
    dm4c.add_listener("post_show_association", function(assoc) {
        topicmap.add_association(assoc.id, assoc.type_uri, assoc.role_1.topic_id, assoc.role_2.topic_id)
    })

    /**
     * @param   topic   a TopicView object
     */
    dm4c.add_listener("post_hide_topic", function(topic) {
        topicmap.hide_topic(topic.id)
    })

    /**
     * @param   assoc   a AssociationView object
     */
    dm4c.add_listener("post_hide_association", function(assoc) {
        topicmap.hide_association(assoc.id)
    })

    /**
     * @param   topic   a TopicView object
     */
    dm4c.add_listener("post_move_topic", function(topic) {
        topicmap.move_topic(topic.id, topic.x, topic.y)
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        // 1) Update all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Updating topic " + topic.id + " on all topicmaps")
        all_topicmaps(function(topicmap) {
            topicmap.update_topic(topic)
        })
        // 2) Update the topicmap menu
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            refresh_topicmap_menu()
        }
    })

    /**
     * @param   assoc       an Association object
     */
    dm4c.add_listener("post_update_association", function(assoc) {
        if (LOG_TOPICMAPS) dm4c.log("Updating association " + assoc.id + " on all topicmaps")
        all_topicmaps(function(topicmap) {
            topicmap.update_association(assoc)
        })
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_delete_topic", function(topic) {
        // 1) Update all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Deleting topic " + topic.id + " from all topicmaps")
        all_topicmaps(function(topicmap) {
            topicmap.delete_topic(topic.id)
        })
        // 2) Update the topicmap menu
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            // remove topicmap model
            delete topicmap_cache[topic.id]
            //
            var topicmap_id = get_topicmap_id_from_menu()
            if (topicmap_id == topic.id) {
                if (LOG_TOPICMAPS) dm4c.log("..... updating the topicmap menu and selecting the first item " +
                    "(the deleted topic was the CURRENT topicmap)")
                if (!js.size(topicmap_cache)) {     // ### FIXME: should check topicmap_topics?
                    create_topicmap_topic("untitled")
                }
                refresh_topicmap_menu()
                set_selected_topicmap(get_topicmap_id_from_menu())
                display_topicmap()
            } else {
                if (LOG_TOPICMAPS) dm4c.log("..... updating the topicmap menu and restoring the selection " +
                    "(the deleted topic was ANOTHER topicmap)")
                refresh_topicmap_menu()
            }
        }
    })

    dm4c.add_listener("post_delete_association", function(assoc) {
        // Remove association from all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Deleting association " + assoc.id + " from all topicmaps")
        all_topicmaps(function(topicmap) {
            topicmap.delete_association(assoc.id)
        })
    })

    dm4c.add_listener("pre_push_history", function(history_entry) {
        history_entry.state.topicmap_id = topicmap.get_id()
        history_entry.url = "/topicmap/" + topicmap.get_id() + history_entry.url
    })

    dm4c.add_listener("pre_pop_history", function(state) {
        if (dm4c.LOG_HISTORY) dm4c.log("..... topicmaps_plugin.pre_pop_history()")
        if (state.topicmap_id != topicmap.get_id()) {
            if (dm4c.LOG_HISTORY) dm4c.log(".......... switch from topicmap " + topicmap.get_id() +
                " to " + state.topicmap_id)
            self.do_select_topicmap(state.topicmap_id, true)    // no_history_update=true
            return false
        } else if (!state.topic_id) {
            if (dm4c.LOG_HISTORY) dm4c.log(".......... topicmap not changed and no topic in popstate " +
                "=> resetting selection")
            dm4c.do_reset_selection(true)                       // no_history_update=true
            return false
        }
    })

    dm4c.add_listener("post_move_cluster", function(cluster) {
        topicmap.move_cluster(cluster)
    })

    dm4c.add_listener("post_move_canvas", function(trans_x, trans_y) {
        topicmap.set_translation(trans_x, trans_y)
    })

    dm4c.add_listener("pre_draw_canvas", function(ctx) {
        // Note: topicmap is undefined if canvas draw() is performed
        // before the Topicmaps plugin is initialized.
        topicmap && topicmap.draw_background(ctx)
    })



    // === Access Control Listeners ===

    dm4c.add_listener("logged_in", function(username) {
        refresh_topicmap_menu()
        // Note: the topicmap permissions are refreshed in the course of refetching the topicmap topics.
        //
        clear_topicmap_cache()
        reload_topicmap()
    })

    dm4c.add_listener("logged_out", function() {
        refresh_topicmap_menu()
        // Note: the topicmap permissions are refreshed in the course of refetching the topicmap topics.
        //
        clear_topicmap_cache()
        reload_topicmap()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The selected topicmap
     */
    this.get_topicmap = function() {
        return topicmap
    }



    // ******************
    // *** Controller ***
    // ******************



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

    // ---

    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  the topicmap topic.
     */
    this.do_create_topicmap = function(name, topicmap_renderer_uri) {
        return create_topicmap(name, topicmap_renderer_uri)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    // *************************
    // *** Controller Helper ***
    // *************************



    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  the topicmap topic.
     */
    function create_topicmap(name, topicmap_renderer_uri) {
        var topicmap_topic = create_topicmap_topic(name, topicmap_renderer_uri)
        refresh_topicmap_menu(topicmap_topic.id)
        // update model
        set_selected_topicmap(topicmap_topic.id)
        // update view
        display_topicmap()
        //
        return topicmap_topic
    }

    /**
     * Reloads the current topicmap from DB and displays it.
     */
    function reload_topicmap() {
        // 1) update model
        // Note: set_selected_topicmap() is not called here as the topicmap cache must be ignored.
        // (Furthermore the cookie and the renderer are already up-to-date).
        topicmap = load_topicmap(topicmap.get_id())
        // 2) update view
        display_topicmap()
    }



    // *************
    // *** Model ***
    // *************



    /**
     * Returns a topicmap from the cache.
     * If not in cache, the topicmap is loaded and cached before.
     */
    function get_topicmap(topicmap_id) {
        // load if not in cache
        if (!topicmap_cache[topicmap_id]) {
            topicmap_cache[topicmap_id] = load_topicmap(topicmap_id)
        }
        //
        return topicmap_cache[topicmap_id]
    }

    /**
     * Updates the model to reflect the given topicmap is now selected.
     */
    function set_selected_topicmap(topicmap_id) {
        if (LOG_TOPICMAPS) dm4c.log("Selecting topicmap " + topicmap_id)
        //
        // 1) update cookie
        // Note: the cookie must be set *before* the topicmap is loaded.
        // Server-side topic loading might depend on the topicmap type.
        js.set_cookie("dm4_topicmap_id", topicmap_id)
        //
        // 2) update "topicmap_renderer"
        // Note: the renderer must be set *before* the topicmap is loaded.
        // The renderer is responsible for loading.
        var renderer_uri = topicmap_topics[topicmap_id].get("dm4.topicmaps.topicmap_renderer_uri")
        topicmap_renderer = get_topicmap_renderer(renderer_uri)
        //
        // 3) update "topicmap"
        topicmap = get_topicmap(topicmap_id)
    }

    function fetch_topicmap_topics() {
        var topics = dm4c.restc.get_topics("dm4.topicmaps.topicmap", true).items    // fetch_composite=true
        topicmap_topics = dm4c.hash_by_id(dm4c.build_topics(topics))
        // ### FIXME: object properties are not sorted
    }

    /**
     * Loads a topicmap from DB.
     *
     * Prerequisite: the topicmap renderer responsible for loading is already set.
     *
     * @return  a TopicmapViewmodel object
     */
    function load_topicmap(topicmap_id) {
        // prepare config
        var config = {
            is_writable: dm4c.has_write_permission_for_topic(topicmap_topics[topicmap_id])
        }
        // load topicmap
        return topicmap_renderer.load_topicmap(topicmap_id, config)
    }

    /**
     * Creates a Topicmap topic in the DB.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  The created topic.
     */
    function create_topicmap_topic(name, topicmap_renderer_uri) {
        topicmap_renderer_uri = topicmap_renderer_uri || "dm4.webclient.default_topicmap_renderer"
        //
        if (LOG_TOPICMAPS) dm4c.log("Creating topicmap \"" + name + "\" (topicmap_renderer_uri=\"" +
            topicmap_renderer_uri + "\")")
        //
        var topicmap_topic = dm4c.restc.create_topicmap(name, topicmap_renderer_uri)
        //
        if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap_topic.id)
        //
        return topicmap_topic
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



    // === Topicmap Cache ===

    function all_topicmaps(visitor_func) {
        for (var topicmap_id in topicmap_cache) {
            visitor_func(topicmap_cache[topicmap_id])
        }
    }

    function clear_topicmap_cache() {
        topicmap_cache = {}
    }



    // ************
    // *** View ***
    // ************



    /**
     * Displays the selected topicmap on the canvas.
     *
     * Prerequisite: the topicmap is already selected in the topicmap menu.
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
            if (LOG_TOPICMAPS) dm4c.log("Switching topicmap renderer \"" +
                renderer_uri + "\" => \"" + new_renderer_uri + "\"")
            dm4c.topicmap_renderer = topicmap_renderer
            dm4c.split_panel.set_left_panel(topicmap_renderer)
        }
    }

    // === Topicmap Menu ===

    /**
     * Re-fetches the topicmap topics and re-populates the topicmap menu.
     *
     * @param   topicmap_id     Optional: ID of the topicmap to select.
     *                          If not given, the current selection is preserved.
     */
    function refresh_topicmap_menu(topicmap_id) {
        if (!topicmap_id) {
            topicmap_id = get_topicmap_id_from_menu()
        }
        //
        fetch_topicmap_topics()
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
        return item && item.value
    }
})
