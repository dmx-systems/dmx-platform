/**
 * Base class for widgets that render a topicmap (in general the left part of the DeepaMehta window).
 *
 * Subclasses override specific adapter methods.
 */
function TopicmapRenderer() {

    // ------------------------------------------------------------------------------------------------- Adapter Methods

    this.get_info = function() {}

    /**
     * @param   refresh_canvas      Optional: if true, the canvas is refreshed.
     */
    this.add_topic = function(topic, refresh_canvas) {}

    this.add_association = function(assoc, refresh_canvas) {}

    this.update_topic = function(topic, refresh_canvas) {}

    this.update_association = function(assoc, refresh_canvas) {}

    this.remove_topic = function(id, refresh_canvas) {}

    /**
     * Removes an association from the canvas (model) and optionally refreshes the canvas (view).
     * If the association is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.remove_association = function(id, refresh_canvas) {}

    // ### FIXME: highlight methods?

    this.scroll_topic_to_center = function(topic_id) {}

    this.begin_association = function(topic_id, x, y) {}

    // ### FIXME: get_associations()?

    this.refresh = function() {}

    this.clear = function() {}

    this.close_context_menu = function() {}

    // --- Grid Positioning ---

    this.start_grid_positioning = function() {}

    this.stop_grid_positioning = function() {}
}
