/**
 * Extends the default canvas renderer (as provided by the webclient module) by persistence.
 */
function CanvasRendererExtension() {

    // === CanvasRenderer Topicmaps Extension ===

    this.load_topicmap = function(topicmap_id, config) {
        return new Topicmap(topicmap_id, config)
    }

    /**
     * @param   no_history_update   Optional: boolean.
     */
    this.display_topicmap = function(topicmap, no_history_update) {

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

            dm4c.canvas.clear()
            dm4c.canvas.translate(topicmap.trans_x, topicmap.trans_y)
            display_topics()
            display_associations()
            restore_selection()     // includes canvas refreshing

            function display_topics() {
                topicmap.iterate_topics(function(topic) {
                    if (topic.visibility) {
                        // Note: canvas.add_topic() expects an topic object with "value" property (not "label")
                        var t = {id: topic.id, type_uri: topic.type_uri, value: topic.label, x: topic.x, y: topic.y}
                        dm4c.canvas.add_topic(t)
                    }
                })
            }

            function display_associations() {
                topicmap.iterate_associations(function(assoc) {
                    var a = {
                        id: assoc.id,
                        type_uri: assoc.type_uri,
                        role_1: {topic_id: assoc.topic_id_1},
                        role_2: {topic_id: assoc.topic_id_2}
                    }
                    dm4c.canvas.add_association(a)
                })
            }

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

    this.initial_topicmap_state = function() {
        return {
            "dm4.topicmaps.translation": {
                "dm4.topicmaps.translation_x": 0,
                "dm4.topicmaps.translation_y": 0
            }
        }
    }
}
