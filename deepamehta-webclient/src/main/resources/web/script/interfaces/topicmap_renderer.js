/**
 * Base class for widgets that render a topicmap (in general the left part of the DeepaMehta window).
 *
 * Subclasses override specific adapter methods.
 */
function TopicmapRenderer() {

    // ------------------------------------------------------------------------------------------------- Adapter Methods

    this.get_info = function() {}

    /**
     * Triggered once the renderer has been added to the DOM.
     */
    this.init = function() {}

    /**
     * Triggered every time the renderer has been added to the DOM.
     */
    this.activate = function() {}

    // ---

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

    this.set_highlight_object = function(object_id, refresh_canvas) {}

    this.reset_highlighting = function(refresh_canvas) {}

    this.scroll_topic_to_center = function(topic_id) {}

    this.begin_association = function(topic_id, x, y) {}

    // ### FIXME: get_associations()?

    this.refresh = function() {}

    this.clear = function() {}

    this.resize = function(size) {}

    this.close_context_menu = function() {}

    // --- Grid Positioning ---

    this.start_grid_positioning = function() {}

    this.stop_grid_positioning = function() {}
}
