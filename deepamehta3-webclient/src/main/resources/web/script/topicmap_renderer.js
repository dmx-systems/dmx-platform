/**
 * Base class for widgets that render a topicmap (in general the left part of the DeepaMehta window).
 *
 * Subclasses override specific adapter methods.
 */
function TopicmapRenderer() {

    var self = this
    calculate_canvas_size()
    // alert("TopicmapRenderer(): width=" + this.canvas_width + " height=" + this.canvas_height)
    $("#canvas-panel").resizable({handles: "e", resize: resize, stop: stop_resize})

    // ------------------------------------------------------------------------------------------------- Adapter Methods

    /**
     * @param   highlight_topic     Optional: if true, the topic is highlighted.
     * @param   refresh_canvas      Optional: if true, the canvas is refreshed.
     * @param   x                   Optional
     * @param   y                   Optional
     */
    this.add_topic = function(id, type, label, highlight_topic, refresh_canvas, x, y) {}

    this.add_relation = function(id, doc1_id, doc2_id, refresh_canvas) {}

    this.remove_topic = function(id, refresh_canvas, is_part_of_delete_operation) {}

    /**
     * Removes a relation from the canvas (model) and optionally refreshes the canvas (view).
     * If the relation is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.remove_relation = function(id, refresh_canvas, is_part_of_delete_operation) {}

    this.remove_all_relations_of_topic = function(topic_id, is_part_of_delete_operation) {}

    this.set_topic_label = function(id, label) {}

    this.scroll_topic_to_center = function(topic_id) {}

    this.refresh = function() {}

    this.close_context_menu = function() {}

    this.begin_relation = function(doc_id, event) {}

    this.clear = function() {}

    this.resize = function() {}

    // --- Grid Positioning ---

    this.start_grid_positioning = function() {}

    this.stop_grid_positioning = function() {}

    // ------------------------------------------------------------------------------------------------------ Public API

    this.adjust_size = function() {
        calculate_canvas_size()
        this.resize()
    }

    // -------------------------------------------------------------------------------------------- Protected Properties

    // this.canvas_width  = 0      // reflects canvas width (in pixel)
    // this.canvas_height = 0      // reflects canvas height (in pixel)

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Calculates the canvas size based on window size and (fixed) detail panel size.
     *
     * Stores the result in the "canvas_width" and "canvas_height" properties.
     */
    function calculate_canvas_size() {
        var w_w = window.innerWidth
        var w_h = window.innerHeight
        var t_h = $("#upper-toolbar").height()
        self.canvas_width = w_w - detail_panel_width - 50   // 35px = 1.2em + 2 * 8px = 19(.2)px + 16px.
                                            // Update: Safari 4 needs 15 extra pixel (for potential vertical scrollbar?)
        self.canvas_height = w_h - t_h - 76 // was 60, then 67 (healing login dialog), then 76 (healing datepicker)
        if (dm3c.LOG_GUI) {
            dm3c.log("Calculating canvas size: window size=" + w_w + "x" + w_h + " toolbar height=" + t_h)
            dm3c.log("..... resulting canvas size=" + self.canvas_width + "x" + self.canvas_height)
        }
    }

    function calculate_detail_panel_size() {
        var w_w = window.innerWidth
        detail_panel_width = w_w - self.canvas_width - 50   // -50px: see above
    }

    // === Event Handling ===

    /**
     * Triggered when the user resizes the canvas (by moving the split pane's resizable-handle).
     */
    function resize(event, ui_event) {
        if (dm3c.LOG_GUI) dm3c.log("Canvas resized: original with=" + ui_event.originalSize.width +
                                                   " current with=" + ui_event.size.width)
        // resize canvas
        self.canvas_width = ui_event.size.width
        self.resize()
        // resize detail panel
        calculate_detail_panel_size()
        $("#detail-panel").width(detail_panel_width)
    }

    function stop_resize() {
        // While resizing-via-handle jQuery UI adds a "style" attribute with absolute size values to the canvas-panel.
        // This stops its flexible sizing (that follows the canvas element's size) and breaks the layout once the main
        // window is resized. Removing that style attribute once resizing-via-handle is finished solves that problem.
        $("#canvas-panel").removeAttr("style")
    }
}
