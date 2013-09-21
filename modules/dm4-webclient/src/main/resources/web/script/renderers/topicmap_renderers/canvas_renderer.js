/**
 * A topicmap renderer that utilizes a HTML5 Canvas.
 */
function CanvasRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    js.extend(this, TopicmapRenderer)

    // Viewmodel
    var topicmap                    // the topicmap currently rendered (a TopicmapViewmodel).
                                    // Initialized by display_topicmap().

    // View (HTML5 Canvas)
    var canvas = new CanvasView()

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
            restore_selection()     // includes canvas refreshing

            function restore_selection() {
                var id = topicmap.selected_object_id
                if (id != -1) {
                    if (topicmap.is_topic_selected) {
                        dm4c.do_select_topic(id, no_history_update)         // includes canvas refreshing
                    } else {
                        dm4c.do_select_association(id, no_history_update)   // includes canvas refreshing
                    }
                } else {
                    dm4c.do_reset_selection(no_history_update)              // includes canvas refreshing
                }
            }
        }
    }

    // ---

    /**
     * Adds a topic to the canvas. If the topic is already on the canvas it is not added again.
     * Note: the canvas is not refreshed.
     *
     * @param   topic       A Topic object with optional "x", "y" properties.
     *                      (Instead a Topic any object with "id", "type_uri", and "value" properties is suitable.)
     * @param   do_select   Optional: if true, the topic is selected.
     */
    this.add_topic = function(topic, do_select) {
        canvas.init_topic_position(topic)
        // update viewmodel
        var topic_viewmodel = topicmap.add_topic(topic.id, topic.type_uri, topic.value, topic.x, topic.y)
        // update view
        if (topic_viewmodel) {
            canvas.add_topic(topic_viewmodel)
        }
        //
        if (do_select) {
            // update viewmodel
            topicmap.set_topic_selection(topic.id)
        }
        //
        return topic
    }

    /**
     * @param   assoc       An Association object.
     *                      (Instead an Association any object with "id", "type_uri", "role_1", "role_2" properties
     *                      is suitable)
     * @param   do_select   Optional: if true, the association is selected.
     */
    this.add_association = function(assoc, do_select) {
        // update viewmodel
        var assoc_viewmodel = topicmap.add_association(assoc.id, assoc.type_uri, assoc.role_1.topic_id,
                                                                                 assoc.role_2.topic_id)
        // update view
        if (assoc_viewmodel) {
            canvas.add_association(assoc_viewmodel)
        }
        //
        if (do_select) {
            // update viewmodel
            topicmap.set_association_selection(assoc.id)
        }
    }

    // ---

    /**
     * Updates a topic. If the topic is not on the canvas nothing is performed. ### FIXDOC
     *
     * @param   topic       A Topic object.
     */
    this.update_topic = function(topic, refresh_canvas) {
        // update viewmodel
        var topic_viewmodel = for_all_topicmaps("update_topic", topic)
        // update view
        if (topic_viewmodel) {
            canvas.update_topic(topic_viewmodel)
            if (refresh_canvas) {
                this.refresh()
            }
        }
    }

    /**
     * Updates an association. If the association is not on the canvas nothing is performed. ### FIXDOC
     *
     * @param   assoc       An Association object.
     */
    this.update_association = function(assoc, refresh_canvas) {
        // update viewmodel
        var assoc_viewmodel = for_all_topicmaps("update_association", assoc)
        // update view
        if (assoc_viewmodel) {
            canvas.update_association(assoc_viewmodel)
            if (refresh_canvas) {
                this.refresh()
            }
        }
    }

    // ---

    /**
     * Removes a topic from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the topic is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.hide_topic = function(topic_id, refresh_canvas) {
        // update viewmodel
        topicmap.hide_topic(topic_id)
        // update view
        canvas.remove_topic(topic_id)
        if (refresh_canvas) {
            this.refresh()
        }
    }

    /**
     * Removes an association from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the association is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.hide_association = function(assoc_id, refresh_canvas) {
        // update viewmodel
        topicmap.hide_association(assoc_id)
        // update view
        canvas.remove_association(assoc_id)
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ---

    this.delete_topic = function(topic_id, refresh_canvas) {
        // update viewmodel
        for_all_topicmaps("delete_topic", topic_id)
        // update view
        canvas.remove_topic(topic_id)
        if (refresh_canvas) {
            this.refresh()
        }
    }

    this.delete_association = function(assoc_id, refresh_canvas) {
        // update viewmodel
        for_all_topicmaps("delete_association", assoc_id)
        // update view
        canvas.remove_association(assoc_id)
        if (refresh_canvas) {
            this.refresh()
        }
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
        // ### FIXME: refresh view?
        //
        return {select: topic, display: topic}
    }

    this.select_association = function(assoc_id) {
        // fetch from DB
        var assoc = dm4c.fetch_association(assoc_id)
        // update viewmodel
        topicmap.set_association_selection(assoc_id)
        // ### FIXME: refresh view?
        //
        return assoc
    }

    this.reset_selection = function(refresh_canvas) {
        // update viewmodel
        topicmap.reset_selection()
        // update view
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ---

    this.scroll_topic_to_center = function(topic_id) {
        var ct = canvas.get_topic(topic_id)
        canvas.scroll_to_center(ct.x + topicmap.trans_x, ct.y + topicmap.trans_y)
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
