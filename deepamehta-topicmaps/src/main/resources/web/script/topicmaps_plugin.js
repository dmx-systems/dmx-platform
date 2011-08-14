function topicmaps_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.topicmaps/style/topicmaps.css")

    var LOG_TOPICMAPS = false

    // model
    var topicmaps = {}  // The topicmaps cache (key: topicmap ID, value: Topicmap object)
    var topicmap        // Selected topicmap (Topicmap object)

    // view
    var topicmap_menu

    // ------------------------------------------------------------------------------------------------ Overriding Hooks



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {

        extend_rest_client()

        var topicmaps = get_all_topicmaps()
        create_default_topicmap()
        create_topicmap_menu()
        create_topicmap_dialog()
        select_initial_topicmap()

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
            var topicmap_dialog = $("<div>").attr("id", "topicmap_dialog")
            var input = $("<input>").attr({id: "topicmap_name", size: 30})
            topicmap_dialog.append("Title:")
            topicmap_dialog.append($("<form>").attr("action", "#").submit(do_create_topicmap).append(input))
            $("body").append(topicmap_dialog)
            $("#topicmap_dialog").dialog({modal: true, autoOpen: false, draggable: false, resizable: false, width: 350,
                title: "New Topicmap", buttons: {"OK": do_create_topicmap}})
        }

        function select_initial_topicmap() {
            if (location.search.match(/topicmap=(\d+)/)) {
                var topicmap_id = RegExp.$1
                select_menu_item(topicmap_id)
            } else {
                var topicmap_id = get_topicmap_id_from_menu()
            }
            // update model
            select_topicmap(topicmap_id)
            // update view
            display_topicmap()
        }
    }

    this.post_select_topic = function(topic) {
        topicmap.set_topic_selection(topic)
    }

    this.post_select_association = function(assoc) {
        topicmap.set_association_selection(assoc)
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
        if (LOG_TOPICMAPS) dm4c.log("Updating topic " + topic.id + " on all topicmaps")
        for (var id in topicmaps) {
            topicmaps[id].update_topic(topic)
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
        // 1) Remove topic from all topicmap models
        if (LOG_TOPICMAPS) dm4c.log("Deleting topic " + topic.id + " from all topicmaps")
        for (var id in topicmaps) {
            topicmaps[id].delete_topic(topic.id)
        }
        // 2) Update the topicmap menu if the deleted topic was a topicmap
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
                select_topicmap(get_topicmap_id_from_menu())
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
     */
    this.do_select_topicmap = function(topicmap_id) {
        // update model
        select_topicmap(topicmap_id)
        // update view
        select_menu_item(topicmap_id)
        display_topicmap()
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
            select_topicmap(topicmap_id)
            // update view
            display_topicmap()
        }
    }

    // ---

    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @return  the topicmap topic.
     */
    this.do_create_topicmap = function(name) {
        return create_topicmap(name)
    }

    function do_create_topicmap() {
        $("#topicmap_dialog").dialog("close")
        var name = $("#topicmap_name").val()
        create_topicmap(name)
        return false
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
        select_topicmap(topicmap_id)
        // update view
        display_topicmap()
    }



    /*************************/
    /*** Controller Helper ***/
    /*************************/



    /**
     * Creates a topicmap with the given name, puts it in the topicmap menu, and displays the topicmap.
     *
     * @return  the topicmap topic.
     */
    function create_topicmap(name) {
        var topicmap_topic = create_topicmap_topic(name)
        rebuild_topicmap_menu(topicmap_topic.id)
        // update model
        select_topicmap(topicmap_topic.id)
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

    function select_topicmap(topicmap_id) {
        if (LOG_TOPICMAPS) dm4c.log("Selecting topicmap " + topicmap_id)
        // update model
        topicmap = load_topicmap(topicmap_id)
    }

    function get_all_topicmaps() {
        return dm4c.restc.get_topics("dm4.topicmaps.topicmap", true)    // sort=true
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
     * Creates a new empty topicmap in the DB.
     */
    function create_topicmap_topic(name) {
        if (LOG_TOPICMAPS) dm4c.log("Creating topicmap \"" + name + "\"")
        var topicmap = dm4c.create_topic("dm4.topicmaps.topicmap", {"dm4.topicmaps.name": name})
        if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.id)
        return topicmap
    }



    /************/
    /*** View ***/
    /************/



    /**
     * Displays the selected topicmap on the canvas.
     *
     * Prerequisite: the topicmap is already selected in the topicmap menu.
     */
    function display_topicmap() {
        topicmap.display_on_canvas()
    }

    // ---

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
        $("#topicmap_dialog").dialog("open")
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * @param   topicmap_id     Optional: ID of the topicmap to select.
     *                          If not given, the current selection is maintained.
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



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * An in-memory representation (model) of a persistent topicmap. There are methods for:
     *  - building the in-memory representation by loading a topicmap from DB.
     *  - displaying the in-memory representation on the canvas.
     *  - manipulating the in-memory representation by e.g. adding/removing topics and associations,
     *    while synchronizing the DB accordingly.
     */
    function Topicmap(topicmap_id) {

        // Model
        var topics = {}     // topics of this topicmap (key: topic ID, value: TopicmapTopic object)
        var assocs = {}     // associations of this topicmap (key: association ID, value: TopicmapAssociation object)
        var selected_object_id = -1     // ID of the selected topic or association, or -1 for no selection
        var is_topic_selected           // true indicates topic selection, false indicates association selection
                                        // only evaluated if there is a selection (selected_object_id != -1)
        load()

        // --- Public API ---

        this.get_id = function() {
            return topicmap_id
        }

        this.display_on_canvas = function() {

            // track loading of topic type images
            var image_tracker = dm4c.create_image_tracker(display_on_canvas)
            for (var id in topics) {
                var topic = topics[id]
                if (topic.visibility) {
                    image_tracker.add_type(topic.type_uri)
                }
            }
            image_tracker.check()

            function display_on_canvas() {

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
                            dm4c.do_select_topic(selected_object_id)
                        } else {
                            dm4c.do_select_association(selected_object_id)
                        }
                    } else {
                        dm4c.do_reset_selection()
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

        this.get_topic = function(id) {
            return topics[id]
        }

        // --- Private Functions ---

        function load() {

            if (LOG_TOPICMAPS) dm4c.log("Loading topicmap " + topicmap_id)

            var topicmap = dm4c.restc.get_topicmap(topicmap_id)

            if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.topics.length + " topics")
            load_topics()

            if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.assocs.length + " associations")
            load_associations()

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
