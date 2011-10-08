function SplitPanel() {

    var left_panel_parent  = $("<td>").append($("<div>", {id: "canvas-panel"}))
    var right_panel_parent = $("<td>")

    var left_panel
    var left_panel_size = {}

    this.dom = $("<table>", {id: "split-panel"}).append($("<tr>")
        .append(left_panel_parent)
        .append(right_panel_parent)
    )

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @param   panel   an object width a "dom" property and a "resize" method.
     */
    this.set_left_panel = function(panel) {
        left_panel = panel
        $("#canvas").remove()               // FIXME: don't rely on ID #canvas
        $("#canvas-panel").append(panel.dom)
    }

    /**
     * @param   panel   an object width a "dom" property.
     */
    this.set_right_panel = function(panel) {
        right_panel_parent.empty()
        right_panel_parent.append(panel.dom)
        //
        detail_panel_width = panel.dom.width()     // ### FIXME: make global variable a PagePanel property!
        if (dm4c.LOG_GUI) dm4c.log("Mesuring page panel width: " + detail_panel_width)
        //
        calculate_canvas_size()
        // alert("TopicmapRenderer(): width=" + this.canvas_width + " height=" + this.canvas_height)
        $("#canvas-panel").resizable({handles: "e", resize: do_resize, stop: do_stop_resize})
        //
        $(window).resize(do_window_resize)
    }

    // ---

    this.get_left_panel_size = function() {
        return left_panel_size
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Calculates the canvas size based on window size and (fixed) page panel size.
     *
     * Stores the result in "left_panel_size".
     */
    function calculate_canvas_size() {
        var w_w = window.innerWidth
        var w_h = window.innerHeight
        var t_h = dm4c.toolbar.dom.height()
        left_panel_size.width  = w_w - detail_panel_width - 41   // 41px = 1.6em + 2 * 8px = 25(.6)px + 16px.
        left_panel_size.height = w_h - t_h - 76 // was 60, then 67 (healing login dialog), then 76 (healing datepicker)
        if (dm4c.LOG_GUI) {
            dm4c.log("Calculating canvas size: window size=" + w_w + "x" + w_h + " toolbar height=" + t_h)
            dm4c.log("..... resulting canvas size=" + JSON.stringify(left_panel_size))
        }
    }

    function calculate_detail_panel_size() {
        var w_w = window.innerWidth
        detail_panel_width = w_w - left_panel_size.width - 41   // 41px: see above
    }

    // === Event Handling ===

    /**
     * Triggered while the user moves the split pane's resizable-handle.
     */
    function do_resize(event, ui_event) {
        if (dm4c.LOG_GUI) dm4c.log("Canvas resized: original with=" + ui_event.originalSize.width +
                                                   " current with=" + ui_event.size.width)
        // resize canvas
        left_panel_size.width = ui_event.size.width
        left_panel.resize(left_panel_size)
        // resize page panel
        calculate_detail_panel_size()
        $("#page-content").width(detail_panel_width)
    }

    /**
     * Triggered while the user releases the split pane's resizable-handle.
     */
    function do_stop_resize() {
        // While resizing-via-handle jQuery UI adds a "style" attribute with absolute size values to the canvas-panel.
        // This stops its flexible sizing (that follows the canvas element's size) and breaks the layout once the main
        // window is resized. Removing that style attribute once resizing-via-handle is finished solves that problem.
        $("#canvas-panel").removeAttr("style")
    }

    // ---

    /**
     * Triggered while the user resizes tge browser window.
     */
    function do_window_resize() {
        calculate_canvas_size()
        left_panel.resize(left_panel_size)
        $("#page-content").height($("#canvas").height())
    }
}
