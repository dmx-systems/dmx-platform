function SplitPanel() {

    var left_panel_parent  = $("<td>").append($("<div>", {id: "canvas-panel"}))
    var right_panel_parent = $("<td>")

    var left_panel
    var right_panel
    
    var panel_height
    var left_panel_width

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
        right_panel = panel
        right_panel_parent.append(panel.dom)
        //
        adjust_right_panel_height()
        right_panel.width = panel.dom.width()
        if (dm4c.LOG_GUI) dm4c.log("Mesuring page panel width: " + right_panel.width)
        //
        calculate_left_panel_width()
        // alert("TopicmapRenderer(): width=" + this.canvas_width + " height=" + this.canvas_height)
        $("#canvas-panel").resizable({handles: "e", resize: do_resize, stop: do_stop_resize})
        //
        $(window).resize(do_window_resize)
    }

    // ---

    this.get_left_panel_size = function() {
        return get_left_panel_size()
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Size Management ===

    // --- Left Panel ---

    /**
     * Calculates the left panel's width based on window width and right panel's width.
     * Stores the result in "left_panel_width".
     *
     * Called in 2 situations:
     *     1) initial setup
     *     2) the browser window is resized
     */
    function calculate_left_panel_width() {
        // update model
        var w_w = window.innerWidth
        left_panel_width  = w_w - right_panel.width - 41   // 41px = 1.6em + 2 * 8px = 25(.6)px + 16px.
        if (dm4c.LOG_GUI) dm4c.log("Canvas width=" + left_panel_width + " (based on window width " + w_w +
            " and page panel width " + right_panel.width + ")")
    }

    // --- Right Panel ---

    /**
     * Calculates the right panel's width based on window width and left panel's width.
     *
     * Called in 1 situation: the resizable-handle is moved.
     */
    function adjust_right_panel_width() {
        // update model
        var w_w = window.innerWidth
        right_panel.width = w_w - left_panel_width - 41   // 41px: see above
        // update view
        $("#page-content").width(right_panel.width)
    }

    /**
    * Called in 2 situations:
    *     1) initial setup
    *     2) the browser window is resized
     */
    function adjust_right_panel_height() {
        // update model
        calculate_panel_height()
        right_panel.height = panel_height
        // update view
        $("#page-content").height(right_panel.height)

        function calculate_panel_height() {
            var w_h = window.innerHeight
            var t_h = dm4c.toolbar.dom.height()
            panel_height = w_h - t_h - 76 // was 60, then 67 (healing login dialog), then 76 (healing datepicker)
            if (dm4c.LOG_GUI) dm4c.log("Panel height=" + panel_height + " (based on window height " + w_h +
                " and toolbar height " + t_h + ")")
        }
    }

    // ---

    function get_left_panel_size() {
        return {width: left_panel_width, height: panel_height}
    }

    // === Event Handling ===

    /**
     * Triggered repeatedly while the user moves the split pane's resizable-handle.
     */
    function do_resize(event, ui_event) {
        if (dm4c.LOG_GUI) dm4c.log("Canvas resized: original with=" + ui_event.originalSize.width +
                                                   " current with=" + ui_event.size.width)
        // Note: the left panel must be updated first. The left panel width is calculated there.
        // Updating the right panel relies on it.
        //
        // update left panel
        left_panel_width = ui_event.size.width
        left_panel.resize(get_left_panel_size())
        // update right panel
        adjust_right_panel_width()
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
     * Triggered repeatedly while the user resizes tge browser window.
     */
    function do_window_resize() {
        // Note: the right panel must be updated first. The panel height is calculated there.
        // Updating the left panel relies on it.
        //
        // update right panel
        adjust_right_panel_height()
        // update left panel
        calculate_left_panel_width()
        left_panel.resize(get_left_panel_size())
    }
}
