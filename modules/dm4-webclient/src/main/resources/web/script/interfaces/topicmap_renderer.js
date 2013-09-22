/**
 * An abstraction of the view component that occupies the left part of the DeepaMehta window (the "canvas").
 * The abstraction comprises:
 *     - A model of the topics and association that are currently displayed.
 *     - A model for the current selection.
 *     - The view element (the "dom" property).
 *
 * The Webclient and the Topicmaps modules, as well as the SplitPanel are coded to this interface.
 *
 * Note: the concept of a (persistent) topicmap does not appear here.
 * It is only introduced by the Topicmaps module.
 */
function TopicmapRenderer() {

    this.get_info = function() {}

    // ---

    /**
     * @return  the loaded topicmap (a viewmodel).
     */
    this.load_topicmap = function(topicmap_id, config) {}

    /**
     * Called from Topicmaps module (display_topicmap() in plugin main class).
     *
     * @param   topicmap            the topicmap to display (a viewmodel).
     * @param   no_history_update   Optional: boolean.
     */
    this.display_topicmap = function(topicmap, no_history_update) {}

    // ---

    /**
     * Adds a topic to the canvas. If the topic is already on the canvas it is not added again. ### FIXDOC
     *
     * An implementation may decide not to show the given topic but a different one or to show nothing at all.
     *
     * The given topic may or may not provide a geometry hint ("x" and "y" properties). In any case placement is up
     * to the implementation.
     *
     * @param   topic       an object with "id", "type_uri", "value" properties and optional "x", "y" properties.
     * @param   do_select   Optional: if true, the topic is selected.
     *
     * @return  the topic actually shown including the geometry where it is actually shown (a Topic object with
     *          "x" and "y" properties) or "undefined" if no topic is shown.
     */
    this.show_topic = function(topic, do_select) {}

    this.show_association = function(assoc, do_select) {}

    this.update_topic = function(topic) {}

    this.update_association = function(assoc) {}

    this.hide_topic = function(topic_id) {}

    /**
     * Removes an association from the canvas (model) and optionally refreshes the canvas (view). ### FIXDOC
     * If the association is not present on the canvas nothing is performed.
     */
    this.hide_association = function(assoc_id) {}

    this.delete_topic = function(topic_id) {}

    this.delete_association = function(assoc_id) {}

    /**
     * Checks if a topic is visible on the canvas.
     *
     * @return  a boolean
     */
    this.is_topic_visible = function(topic_id) {}



    // === Selection ===

    /**
     * Selects a topic, that is it is rendered as highlighted.
     * If the topic is not present on the canvas nothing is performed. ### FIXDOC (explain): In this case
     * there is still a value returned.
     *
     * Called from Webclient module (do_select_topic()).
     *
     * @return  an object with "select" and "display" properties (both values are Topic objects).
     */
    this.select_topic = function(topic_id) {}

    this.select_association = function(assoc_id) {}

    this.reset_selection = function() {}

    // ---

    this.scroll_topic_to_center = function(topic_id) {}

    this.begin_association = function(topic_id, x, y) {}

    this.refresh = function() {}



    // === Grid Positioning ===

    this.start_grid_positioning = function() {}

    this.stop_grid_positioning = function() {}



    // === Left SplitPanel Component ===

    /**
     * Called by the SplitPanel once this renderer has been added to the DOM.
     */
    this.init = function() {}

    /**
     * @param   size    an object with "width" and "height" properties.
     */
    this.resize = function(size) {}

    this.resize_end = function() {}
}
