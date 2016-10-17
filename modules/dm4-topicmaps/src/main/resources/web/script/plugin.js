dm4c.add_plugin("de.deepamehta.topicmaps", function() {

    dm4c.load_script("/de.deepamehta.topicmaps/script/plugin_model.js")
    dm4c.load_script("/de.deepamehta.topicmaps/script/plugin_view.js")
    dm4c.load_script("/de.deepamehta.topicmaps/script/topicmap_viewmodel.js")

    var model = new TopicmapsPluginModel()
    var view = new TopicmapsPluginView({
        get_topicmap:               model.get_topicmap,
        get_topicmap_renderer:      model.get_current_topicmap_renderer,
        get_topicmap_topics:        model.get_topicmap_topics,
        iterate_topicmap_renderers: model.iterate_topicmap_renderers,
        select_topicmap: select_topicmap,
        create_topicmap: create_topicmap
    })

    var self = this



    // === REST Client Extension ===

    dm4c.restc.create_topicmap = function(name, topicmap_renderer_uri, private) {
        var params = this.queryParams({
            name:         name,
            renderer_uri: topicmap_renderer_uri,
            private:      private,
        })
        return this.request("POST", "/topicmap" + params)
    }

    /**
     * @param   include_childs (boolean)    Optional: if true the topics contained in the returned topicmap will
     *                                      include their child topics. Default is false.
     */
    dm4c.restc.get_topicmap = function(topicmap_id, include_childs) {
        var params = this.queryParams({include_childs: include_childs})
        return this.request("GET", "/topicmap/" + topicmap_id + params)
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
     * Registers the installed topicmap renderers.
     *
     * Note: plugins are supposed to register their view customizers and viewmodel customizers at init_2.
     * Registering the topicmap renderers at init(1) ensures they are available for being customized.
     */
    dm4c.add_listener("init", function() {
        model.register_topicmap_renderers()
    })

    /**
     * Note: the Workspaces plugin initializes at init(1). Initializing the topicmaps model at init_2 ensures
     * the workspace widget is already added to the toolbar and a selected workspace is already known.
     */
    dm4c.add_listener("init_2", function() {
        // init model
        model.init()
        // init view
        view.create_topicmap_widget()
        view.refresh_topicmap_menu()
    })

    /**
     * Displays the initial topicmap.
     *
     * Note: plugins are supposed to register their view customizers and viewmodel customizers at init_2.
     * Displaying the initial topicmap at init_3 ensures all customizers are registered already.
     */
    dm4c.add_listener("init_3", function() {
        view.display_topicmap()                  // ### FIXME: rethink about history update
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        // update the topicmap menu
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            // update model
            model.fetch_topicmap_topics()
            // update view
            view.refresh_topicmap_menu()
        }
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_delete_topic", function(topic) {
        if (topic.type_uri == "dm4.topicmaps.topicmap") {
            // 1) update model
            var is_current_topicmp = model.delete_topicmap(topic.id)
            // 2) update view
            view.refresh_topicmap_menu()
            if (is_current_topicmp) {
                view.display_topicmap()
            }
        }
    })

    dm4c.add_listener("pre_push_history", function(history_entry) {
        var topicmap_id = model.get_topicmap().get_id()
        history_entry.state.topicmap_id = topicmap_id
        history_entry.url = "/topicmap/" + topicmap_id + history_entry.url
    })

    dm4c.add_listener("pre_pop_history", function(state) {
        if (state.topicmap_id != model.get_topicmap().get_id()) {
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
        // update view
        //
        // Note: topicmap is undefined if canvas draw() is performed before the Topicmaps plugin is initialized.
        var topicmap = model.get_topicmap()
        topicmap && topicmap.draw_background(ctx)
    })



    // === Workspace Listeners ===

    dm4c.add_listener("post_select_workspace", function(workspace_id) {
        // 1) update model
        model.select_topicmap_for_workspace(workspace_id)
        //
        // 2) update view
        view.refresh_topicmap_menu()
        view.display_topicmap()
    })



    // === Access Control Listeners ===

    dm4c.add_listener("logged_in", function(username) {
        // 1) update model
        //
        // Note: a user can have private topicmaps in several workspaces.
        // So, when the authority changes the topicmap topics for ALL workspaces must be invalidated.
        model.clear_topicmap_topics()
        // Note: when the authority changes the contents of ALL topicmaps in ALL workspaces are affected.
        // So, the entire topicmap cache must be invalidated.
        model.clear_topicmap_cache()
        //
        // Note: when logging in the user always stays in the selected workspace.
        // The workspace is never programmatically switched in response to a login.
        // So, we refetch the topicmap topics for the CURRENT workspace.
        model.fetch_topicmap_topics()
        // Note: when logging in the user always stays in the selected topicmap.
        // The topicmap is never programmatically switched in response to a login.
        // So, we reload the CURRENT topicmap.
        model.reload_topicmap()
        //
        // 2) update view
        view.refresh_topicmap_menu()
        view.display_topicmap()
    })

    dm4c.add_listener("authority_decreased", function() {
        // Note: a user can have private topicmaps in several workspaces.
        // So, when the authority changes the topicmap topics for ALL workspaces must be invalidated.
        model.clear_topicmap_topics()
        // Note: when the authority changes the contents of ALL topicmaps in ALL workspaces are affected.
        // So, the entire topicmap cache must be invalidated.
        model.clear_topicmap_cache()
        //
        // Note: when the authority decreases the workspace and/or the topicmap may switch programmatically.
        // The topicmap menu and the topicmap is refreshed by the "post_select_workspace" listener (above)
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The selected topicmap
     */
    this.get_topicmap = function() {
        return model.get_topicmap()
    }

    this.iterate_topicmaps = function(visitor_func) {
        model.iterate_topicmap_cache(visitor_func)
    }

    this.get_topicmap_renderer = function(renderer_uri) {
        return model.get_topicmap_renderer(renderer_uri)
    }

    /**
     * Selects a topicmap programmatically.
     * The respective item from the topicmap menu is selected and the topicmap is displayed.
     *
     * @param   no_history_update   Optional: boolean.
     */
    this.select_topicmap = function(topicmap_id, no_history_update) {
        // update model + view
        select_topicmap(topicmap_id, no_history_update)
        // update view
        view.refresh_menu_item()
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
    this.create_topicmap = function(name, topicmap_renderer_uri, private) {
        return create_topicmap(name, topicmap_renderer_uri, private)
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
     * <p>
     * Prerequisite: the topicmap menu already shows the selected topicmap.
     *
     * @param   no_history_update   Optional: boolean.
     */
    function select_topicmap(topicmap_id, no_history_update) {
        // update model
        model.set_selected_topicmap(topicmap_id)
        // update view
        view.display_topicmap(no_history_update)
    }

    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays it.
     *
     * @param   topicmap_renderer_uri   Optional: the topicmap renderer to attach to the topicmap.
     *                                  Default is "dm4.webclient.default_topicmap_renderer".
     *
     * @return  the topicmap topic.
     */
    function create_topicmap(name, topicmap_renderer_uri, private) {
        // update DB
        var topicmap_topic = create_topicmap_topic(name, topicmap_renderer_uri, private)
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
    function create_topicmap_topic(name, topicmap_renderer_uri, private) {
        return dm4c.restc.create_topicmap(name, topicmap_renderer_uri || "dm4.webclient.default_topicmap_renderer",
            private)
    }

    /**
     * Puts a new topicmap in the topicmap menu, selects and displays it.
     * This is called when a new topicmap is created at server-side and now should be displayed.
     */
    function add_topicmap(topicmap_id) {
        // update model
        model.fetch_topicmap_topics()
        model.set_selected_topicmap(topicmap_id)
        // update view
        view.refresh_topicmap_menu()
        view.display_topicmap()
    }
})
// Enable debugging for dynamically loaded scripts:
//# sourceURL=topicmaps_plugin.js
