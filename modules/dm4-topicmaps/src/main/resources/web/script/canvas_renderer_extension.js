/**
 * Extends the default topicmap renderer (as provided by the webclient module) by persistence.
 */
function CanvasRendererExtension() {

    // === TopicmapRenderer Topicmaps Extension ===

    this.load_topicmap = function(topicmap_id) {
        return new Topicmap(topicmap_id)
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
