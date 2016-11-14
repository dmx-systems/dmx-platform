function TopicmapsPluginModel() {

    // --------------------------------------------------------------------------------------------------- Private State

    var topicmap                    // Selected topicmap (TopicmapViewmodel object)     \ updated together by
    var topicmap_renderer           // The topicmap renderer of the selected topicmap   / set_selected_topicmap()
    var topicmap_renderers = {}     // Registered topicmap renderers (key: renderer URI, value: TopicmapRenderer object)
                                    // Initialized by register_topicmap_renderers()
    var topicmap_topics = {}        // Loaded topicmap topics, grouped by workspace, an object:
                                    //   {
                                    //     workspaceId: [topicmapTopic]     # real Topic objects
                                    //   }
                                    // Updated by fetch_topicmap_topics()
    var selected_topicmap_ids = {}  // ID of the selected topicmap, per-workspace, an object:
                                    //   {
                                    //     workspaceId: selectedTopicmapId
                                    //   }
    var topicmap_cache = {}         // Loaded topicmaps (key: topicmap ID, value: TopicmapViewmodel object)

    // ------------------------------------------------------------------------------------------------------ Public API

    this.init = init
    this.init_2 = init_2
    this._get_initial_topicmap_id = function() {return _topicmap_id}
    this._drop_initial_state = drop_initial_state

    this.get_topicmap = function() {return topicmap}
    this.set_selected_topicmap = set_selected_topicmap
    this.select_topicmap_for_workspace = select_topicmap_for_workspace
    this.delete_topicmap = delete_topicmap
    this.reload_topicmap = reload_topicmap

    this.get_topicmap_topics = get_topicmap_topics
    this.fetch_topicmap_topics = fetch_topicmap_topics
    this.clear_topicmap_topics = clear_topicmap_topics

    this.get_topicmap_renderer = get_topicmap_renderer
    this.get_current_topicmap_renderer = function() {return topicmap_renderer}
    this.iterate_topicmap_renderers = iterate_topicmap_renderers

    this.iterate_topicmap_cache = iterate_topicmap_cache
    this.clear_topicmap_cache = clear_topicmap_cache

    this.get_selected_workspace_id = get_selected_workspace_id

    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_selected_workspace_id() {
        return dm4c.get_plugin("de.deepamehta.workspaces").get_selected_workspace_id()
    }



    // === Initial State ===

    // Short-term init state
    var _topicmap_id
    var _topic_id

    // Called from plugin at init(1) stage.
    function init() {
        register_topicmap_renderers()
        //
        // Try to obtain a topicmap ID from browser URL, then from cookie
        var match = location.pathname.match(/\/topicmap\/(\d+)(\/topic\/(\d+))?/)
        if (match) {
            _topicmap_id = match[1]
            _topic_id    = match[3]
            console.log("Selecting topicmap " + _topicmap_id + " (ID obtained from browser URL), selected topic: " +
                _topic_id)
        } else {
            _topicmap_id = js.get_cookie("dm4_topicmap_id")
            if (_topicmap_id) {
                console.log("Selecting topicmap " + _topicmap_id + " (ID obtained from cookie)")
            }
        }
        //
        if (_topicmap_id && !is_valid_topicmap_id(_topicmap_id)) {
            drop_initial_state()    // prompt for the fallback at init(3) stage
        }
        // Note: if at this stage a valid topicmap ID is available already the Workspaces plugin will select the
        // corresponding workspace (at init(2) stage). Otherwise it will select the 1st best workspace and leave it to
        // the Topicmaps plugin to choose a valid topicmap ID for that workspace. This fallback takes place at init(3)
        // stage (see init_2() below).
        //
        // So, there are possible 2 situations: in one the workspace selection relies on the topicmap selection. In the
        // other the topicmap selection relies on the workspace selection. That is what makes the initialization of the
        // Topicmaps plugin so intricate.
        //
        // At this point one edge case might apply:
        //
        // A topicmap ID might be regarded as valid although it is not. This happens if a stale Topicmap topic is
        // obtained from the browser cache. This happens if the GET request's condition (If-Modified-Since) is not
        // satisfied and the response is 304 Not Modified. For this to happen it is sufficient if the pretended (stale)
        // topicmap ID refers to *some* existing DB object (topic or association!) whose modification timestamp is
        // "old enough"! This case is not detectable by the Topicmaps plugin at init(1) stage and will only be detected
        // by the Workspaces plugin (at init(2) stage). A valid topicmap ID will be finally obtained through the
        // fallback at init(3) stage (see init_2() below).

        function is_valid_topicmap_id(id) {
            try {
                var topic = dm4c.restc.request("GET", "/core/topic/" + id, undefined, undefined, undefined, undefined,
                    function() {return false}   // suppress error dialog
                )
                if (topic.type_uri != "dm4.topicmaps.topicmap") {
                    throw "it refers to a \"" + topic.type_uri + "\" topic"
                }
                return true
            } catch (e) {
                console.log("WARNING: " + id + " is not a valid topicmap ID (" + e + ")")
            }
        }
    }

    // Called from plugin at init(3) stage.
    //
    // At this stage we know a workspace is selected, and now we're ready to 1) retrieve the topicmap topics for the
    // selected workspace, and 2) perform the fallback to obtain a valid topicmap ID, if not yet achieved, and 3) select
    // the choosen topicmap finally.
    function init_2() {
        fetch_topicmap_topics()
        //
        // fallback to obtain a valid topicmap ID
        if (!_topicmap_id) {
            _topicmap_id = get_first_topicmap_id()
            console.log("Selecting topicmap " + _topicmap_id + " (ID obtained from 1st menu item)")
        }
        //
        set_selected_topicmap(_topicmap_id)
        if (_topic_id) {
            topicmap.set_topic_selection(_topic_id)
        }
    }

    // ---

    function drop_initial_state() {
        _topicmap_id = undefined
        _topic_id = undefined
    }



    // === Topicmaps ===

    /**
     * Updates the model to reflect the given topicmap is now selected. That includes setting a cookie
     * and updating 3 model objects ("topicmap", "topicmap_renderer", "selected_topicmap_ids").
     * <p>
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
        var renderer_uri = get_topicmap_topic_or_throw(topicmap_id).get("dm4.topicmaps.topicmap_renderer_uri")
        topicmap_renderer = get_topicmap_renderer(renderer_uri)
        //
        // 3) update "topicmap" and "selected_topicmap_ids"
        topicmap = get_topicmap(topicmap_id)
        selected_topicmap_ids[get_selected_workspace_id()] = topicmap_id
    }

    /**
     * Updates the model to reflect the given workspace is now selected.
     */
    function select_topicmap_for_workspace(workspace_id) {
        // Load topicmap topics for that workspace, if not done already
        if (!get_topicmap_topics()) {
            fetch_topicmap_topics()
        }
        // Note: if the last topicmap of that workspace was deleted from "outside" we create the default
        // topicmap lazily (see delete_topicmap()).
        create_default_topicmap()
        //
        // Restore recently selected topicmap for that workspace
        var topicmap_id = selected_topicmap_ids[workspace_id]
        // Choose an alternate topicmap if either no topicmap was selected in that workspace ever, or if
        // the formerly selected topicmap is not available anymore. The latter is the case e.g. when the
        // user logs out while a public/common workspace and a private topicmap is selected.
        if (!topicmap_id || !get_topicmap_topic(topicmap_id)) {
            topicmap_id = get_first_topicmap_id()
        }
        set_selected_topicmap(topicmap_id)
    }

    /**
     * Updates the model to reflect the given topicmap is now deleted.
     *
     * @param   workspace_id    the ID of the workspace the given topicmap was assigned to.
     *                          Note: this is not necessarily the selected workspace.
     */
    function delete_topicmap(topicmap_id, workspace_id) {
        var is_current_topicmap = topicmap_id == topicmap.get_id()
        invalidate_topicmap_cache(topicmap_id)
        remove_topicmap_topic(topicmap_id, workspace_id)
        // If the topicmap was deleted from "within" (that is the workspace the topicmap was assigned to is currently
        // SELECTED) we might need to select another topicmap now. This possibly requires to create a default topicmap
        // first.
        // In contrast, if the topicmap was deleted from "outside" (that is the workspace the topicmap was assigned to
        // is NOT currently selected) we do NOT create the default topicmap here. It would be assigned to the wrong
        // workspace. The default topicmap gets created as soon as the deleted topicmap's workspace is selected (see
        // select_topicmap_for_workspace()).
        if (workspace_id == get_selected_workspace_id()) {
            // Create a default topicmap if the user deleted the workspace's last topicmap.
            create_default_topicmap()
            // If the deleted topicmap was the CURRENT topicmap we must select another one from the topicmap menu.
            if (is_current_topicmap) {
                console.log("Deleted topicmap " + topicmap_id + " was the CURRENT topicmap of workspace " +
                    workspace_id)
                set_selected_topicmap(get_first_topicmap_id())
            }
        }
        return is_current_topicmap
    }

    /**
     * Reloads the current topicmap from DB.
     */
    function reload_topicmap() {
        // Note: the cookie and the renderer are already up-to-date
        var topicmap_id = topicmap.get_id()
        invalidate_topicmap_cache(topicmap_id)
        topicmap = get_topicmap(topicmap_id)
    }



    // === Topicmap Topics ===

    /**
     * Looks up a topicmap topic for the selected workspace.
     *
     * @return  the topicmap topic, or undefined if no such topicmap is available in the selected workspace.
     */
    function get_topicmap_topic(topicmap_id) {
        return js.find(get_topicmap_topics(), function(topic) {
            return topic.id == topicmap_id
        })
    }

    /**
     * Looks up a topicmap topic for the selected workspace.
     * If no such topicmap is available in the selected workspace an exception is thrown.
     *
     * @return  the topicmap topic.
     */
    function get_topicmap_topic_or_throw(topicmap_id) {
        var topicmap_topic = get_topicmap_topic(topicmap_id)
        if (!topicmap_topic) {
            throw "TopicmapsPluginError: topicmap " + topicmap_id + " not found in model for workspace " +
                get_selected_workspace_id()
        }
        return topicmap_topic
    }

    /**
     * Returns the ID of the first loaded topicmap topic for the selected workspace
     */
    function get_first_topicmap_id() {
        var topicmap_topic = get_topicmap_topics()[0]
        if (!topicmap_topic) {
            throw "TopicmapsPluginError: workspace " + get_selected_workspace_id() + " has no topicmaps"
        }
        return topicmap_topic.id
    }

    /**
     * @param   workspace_id    the ID of the workspace the given topicmap was assigned to.
     *                          Note: this is not necessarily the selected workspace.
     */
    function remove_topicmap_topic(topicmap_id, workspace_id) {
        var deleted = js.delete(get_topicmap_topics(workspace_id), function(topic) {
            return topic.id == topicmap_id
        })
        if (deleted != 1) {
            throw "TopicmapsPluginError: removing topicmap " + topicmap_id + " from model of workspace " +
                workspace_id + " failed (" + deleted + " entries deleted)"
        }
    }

    // ---

    /**
     * Returns the loaded topicmap topics for the given workspace.
     *
     * @param   workspace_id    (Optional) If not given the loaded topicmap topics for the
     *                          *selected* workspace are returned.
     */
    function get_topicmap_topics(workspace_id) {
        return topicmap_topics[workspace_id || get_selected_workspace_id()]
    }

    /**
     * Fetches all Topicmap topics assigned to the selected workspace, and updates the model ("topicmap_topics").
     */
    function fetch_topicmap_topics() {
        var workspace_id = get_selected_workspace_id()
        var topics = dm4c.restc.get_assigned_topics(workspace_id, "dm4.topicmaps.topicmap", true, true)
        topicmap_topics[workspace_id] = dm4c.build_topics(topics)                      // include_childs=true, sort=true
    }

    function clear_topicmap_topics() {
        topicmap_topics = {}
    }

    // ---

    /**
     * Checks the model if for the <i>selected workspace</i> topicmap topics exists, and if not creates a new default
     * topicmap.
     * <p>
     * Prerequisite: the topicmap topics of the selected workspace are loaded already (which might result in an empty
     * array though).
     */
    function create_default_topicmap() {
        if (!get_topicmap_topics().length) {
            var topicmap_topic = create_topicmap_topic("untitled")  // renderer=default, private=false
            var workspace_id = get_selected_workspace_id()
            console.log("Creating default topicmap (ID " + topicmap_topic.id + ") for workspace " + workspace_id)
            topicmap_topics[workspace_id].push(new Topic(topicmap_topic))
        }
    }

    function create_topicmap_topic(name, topicmap_renderer_uri, private) {
        return dm4c.restc.create_topicmap(name, topicmap_renderer_uri || "dm4.webclient.default_topicmap_renderer",
            private)
    }



    // === Topicmap Renderers ===

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

    function get_topicmap_renderer(renderer_uri) {
        var renderer = topicmap_renderers[renderer_uri]
        // error check
        if (!renderer) {
            throw "TopicmapsPluginError: \"" + renderer_uri + "\" is an unknown topicmap renderer"
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

    /**
     * Returns a topicmap from the cache.
     * If not in cache, the topicmap is loaded from DB (and then cached).
     */
    function get_topicmap(topicmap_id) {
        var topicmap = topicmap_cache[topicmap_id]
        if (!topicmap) {
            topicmap = load_topicmap(topicmap_id)
            topicmap_cache[topicmap_id] = topicmap
        }
        return topicmap
    }

    /**
     * Loads a topicmap from DB.
     * <p>
     * Prerequisite: the topicmap renderer responsible for loading is already set.
     *
     * @return  the loaded topicmap (a TopicmapViewmodel).
     */
    function load_topicmap(topicmap_id) {
        return topicmap_renderer.load_topicmap(topicmap_id, {
            is_writable: dm4c.has_write_permission_for_topic(topicmap_id)
        })
    }

    // ---

    function iterate_topicmap_cache(visitor_func) {
        for (var topicmap_id in topicmap_cache) {
            visitor_func(topicmap_cache[topicmap_id])
        }
    }

    function invalidate_topicmap_cache(topicmap_id) {
        delete topicmap_cache[topicmap_id]
    }

    function clear_topicmap_cache() {
        topicmap_cache = {}
    }
}
// Enable debugging for dynamically loaded scripts:
//# sourceURL=topicmaps_plugin_model.js
