/**
 * A topicmap renderer that utilizes a HTML5 Canvas.
 */
function CanvasRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    js.extend(this, TopicmapRenderer)

    this.dom = $("<div>")
        .append($("<canvas>").attr("id", "canvas"))
        .append($("<div>").attr("id", "topic-layer"))

    // View (HTML5 Canvas)
    var canvas = new CanvasView()

    // Viewmodel
    var topicmap    // the topicmap currently rendered (a TopicmapViewmodel). Initialized by display_topicmap().
    var viewmodel_customizers = []

    // ------------------------------------------------------------------------------------------------------ Public API



    // === TopicmapRenderer Implementation ===

    this.get_info = function() {
        return {
            uri: "dm4.webclient.default_topicmap_renderer",
            name: "Topicmap"
        }
    }

    // ---

    this.load_topicmap = function(topicmap_id, config) {
        config.customizers = viewmodel_customizers
        return new TopicmapViewmodel(topicmap_id, config)
    }

    this.display_topicmap = function(topicmap_viewmodel, no_history_update) {

        topicmap = topicmap_viewmodel
        track_images()

        /**
         * Defers "display_topicmap" until all topicmap images are loaded.
         */
        function track_images() {
            var image_tracker = dm4c.create_image_tracker(display_topicmap)
            // add type icons
            topicmap.iterate_topics(function(topic) {
                if (topic.visibility) {
                    image_tracker.add_image(dm4c.get_type_icon(topic.type_uri))
                }
            })
            // add background image
            if (topicmap.background_image) {
                image_tracker.add_image(topicmap.background_image)
            }
            //
            image_tracker.check()
        }

        function display_topicmap() {

            canvas.set_topicmap(topicmap)
            restore_selection()

            function restore_selection() {
                var id = topicmap.selected_object_id
                if (id != -1) {
                    if (topicmap.is_topic_selected) {
                        dm4c.do_select_topic(id, no_history_update)
                    } else {
                        dm4c.do_select_association(id, no_history_update)
                    }
                } else {
                    dm4c.do_reset_selection(no_history_update)
                }
            }
        }
    }

    // ---

    /**
     * Adds a topic to the canvas. If the topic is already on the canvas it is not added again. ### FIXDOC
     *
     * @param   topic       A Topic object with optional "x", "y" properties.
     *                      (Any object with "id", "type_uri", and "value" properties is suitable.)
     * @param   do_select   Optional: if true, the topic is selected.
     */
    this.show_topic = function(topic, do_select) {
        canvas.init_topic_position(topic)
        // update viewmodel
        var topic_viewmodel = topicmap.add_topic(topic, topic.x, topic.y)
        if (do_select) {
            topicmap.set_topic_selection(topic.id)
        }
        // update view
        if (topic_viewmodel) {
            canvas.show_topic(topic_viewmodel)
        }
        if (do_select) {
            canvas.set_topic_selection(topic.id)
        }
        //
        return topic
    }

    /**
     * @param   assoc       An Association object.
     *                      (Any object with "id", "type_uri", "role_1", "role_2" properties is suitable.)
     * @param   do_select   Optional: if true, the association is selected.
     */
    this.show_association = function(assoc, do_select) {
        // update viewmodel
        var assoc_viewmodel = topicmap.add_association(assoc)
        if (do_select) {
            topicmap.set_association_selection(assoc.id)
        }
        // update view
        if (assoc_viewmodel) {
            canvas.show_association(assoc_viewmodel)
        }
    }

    // ---

    /**
     * Updates a topic. If the topic is not on the canvas nothing is performed. ### FIXDOC
     *
     * @param   topic       A Topic object.
     */
    this.update_topic = function(topic) {
        // update viewmodel
        var topic_viewmodel = for_all_topicmaps("update_topic", topic)
        // update view
        if (topic_viewmodel) {
            canvas.update_topic(topic_viewmodel)
        }
    }

    /**
     * Updates an association. If the association is not on the canvas nothing is performed. ### FIXDOC
     *
     * @param   assoc       An Association object.
     */
    this.update_association = function(assoc) {
        // update viewmodel
        var assoc_viewmodel = for_all_topicmaps("update_association", assoc)
        // update view
        if (assoc_viewmodel) {
            canvas.update_association(assoc_viewmodel)
        }
    }

    // ---

    /**
     * Removes a topic from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the topic is not present on the canvas nothing is performed.
     */
    this.hide_topic = function(topic_id) {
        // update viewmodel
        topicmap.hide_topic(topic_id)
        // update view
        canvas.remove_topic(topic_id)
    }

    /**
     * Removes an association from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the association is not present on the canvas nothing is performed.
     */
    this.hide_association = function(assoc_id) {
        // update viewmodel
        topicmap.hide_association(assoc_id)
        // update view
        canvas.remove_association(assoc_id)
    }

    // ---

    this.delete_topic = function(topic_id) {
        // update viewmodel
        for_all_topicmaps("delete_topic", topic_id)
        // update view
        canvas.remove_topic(topic_id)
    }

    this.delete_association = function(assoc_id) {
        // update viewmodel
        for_all_topicmaps("delete_association", assoc_id)
        // update view
        canvas.remove_association(assoc_id)
    }

    // ---

    this.is_topic_visible = function(topic_id) {
        var topic = topicmap.get_topic(topic_id)
        return topic && topic.visibility
    }

    // ---

    this.select_topic = function(topic_id) {
        // fetch from DB
        var topic = dm4c.fetch_topic(topic_id)
        // update viewmodel
        topicmap.set_topic_selection(topic_id)
        // update view
        canvas.set_topic_selection(topic_id)
        //
        return {select: topic, display: topic}
    }

    this.select_association = function(assoc_id) {
        // fetch from DB
        var assoc = dm4c.fetch_association(assoc_id)
        // update viewmodel
        topicmap.set_association_selection(assoc_id)
        // update view
        canvas.set_association_selection(assoc_id)
        //
        return assoc
    }

    this.reset_selection = function() {
        // update viewmodel
        topicmap.reset_selection()
        // update view
        canvas.refresh()
    }

    // ---

    this.scroll_topic_to_center = function(topic_id) {
        canvas.scroll_to_center(topic_id)
    }

    this.begin_association = function(topic_id, x, y) {
        canvas.begin_association(topic_id, x, y)
    }

    this.refresh = function() {
        canvas.refresh()
    }



    // === Grid Positioning ===

    this.start_grid_positioning = function() {
        canvas.start_grid_positioning()
    }

    this.stop_grid_positioning = function() {
        canvas.stop_grid_positioning()
    }



    // === Left SplitPanel Component Implementation ===

    /**
     * Called in 2 situations:
     * 1) The user resizes the main window.
     * 2) The user moves the split panel's slider.
     */
    this.resize = function(size) {
        canvas.resize(size)
    }



    // === End of interface implementations ===

    this.add_view_customizer = function(customizer_constructor) {
        canvas.add_view_customizer(customizer_constructor)
    }

    this.add_viewmodel_customizer = function(customizer_constructor) {
        viewmodel_customizers.push(new customizer_constructor())
    }

    // ---

    this.get_topic_associations = function(topic_id) {
        return topicmap.get_topic_associations(topic_id)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    /**
     * Iterates through all topicmaps and calls the given function with the given argument on them.
     * Returns the function call's return value for the topicmap that is currently displayed.
     */
    function for_all_topicmaps(topicmap_func, arg) {
        var return_value
        dm4c.get_plugin("de.deepamehta.topicmaps").iterate_topicmaps(function(_topicmap) {
            var ret = _topicmap[topicmap_func](arg)
            if (topicmap.get_id() == _topicmap.get_id()) {
                return_value = ret
            }
        })
        return return_value
    }
}
