function SplitPanel() {

    var PADDING_MIDDLE = 25     // 25px = 1.6em = 1.6 * 16px = 25(.6)
    var PADDING_BOTTOM = 60     // was 60px, then 67 (healing login dialog), then 76 (healing datepicker)

    var left_panel_parent  = $("<td>").append($("<div>", {id: "topicmap-panel"}))
    var right_panel_parent = $("<td>")

    var left_panel
    var right_panel

    var panel_height
    var left_panel_width

    var left_panel_uris = []    // keeps track of panel initialization

    this.dom = $("<table>", {id: "split-panel"}).append($("<tr>")
        .append(left_panel_parent)
        .append(right_panel_parent)
    )

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @param   panel   an object with a "dom" property and "get_info", "init", "resize", "resize_end" methods.
     */
    this.set_left_panel = function(panel) {
        left_panel = panel
        //
        // 1) add panel to page (by replacing the existing one)
        // Note: class "topicmap-renderer" is added in order to address the panel for removal.
        $("#topicmap-panel .topicmap-renderer").remove()
        $("#topicmap-panel").append(panel.dom.addClass("topicmap-renderer"))
        // Note: re-added panels must be resized to adapt to possibly changed slider position.
        resize_left_panel()
        // Note: resizing takes place *after* replacing. This allows panels to recreate their DOM
        // to realize resizing. Otherwise the panel's event handlers would be lost while replacing.
        //
        // "Setting left panel, dom.width()=" + panel.dom.width() + ", left_panel_width=" + left_panel_width
        //
        // 2) init panel
        var panel_uri = panel.get_info().uri
        // Note: each panel is initialized only once. We recognize panels by their URIs.
        if (!js.contains(left_panel_uris, panel_uri)) {
            left_panel_uris.push(panel_uri)
            panel.init()
        }
    }

    /**
     * @param   panel   an object with a "dom" property.
     */
    this.set_right_panel = function(panel) {
        right_panel = panel
        right_panel_parent.append(panel.dom)
        //
        adjust_right_panel_height()
        right_panel.width = panel.dom.width()
        // "Page panel width=" + right_panel.width
        //
        calculate_left_panel_width()
        $("#topicmap-panel").resizable({handles: "e", resize: do_resize, stop: do_stop_resize})
        //
        $(window).resize(do_window_resize)
    }

    // ---

    this.get_slider_position = function() {
        return get_left_panel_size().width
    }

    this.set_slider_position = function(pos) {
        set_slider_position(pos)
    }

    // ---

    /**
     * @return  an object with "width" and "height" properties.
     */
    this.get_left_panel_size = function() {
        return get_left_panel_size()
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Size Management ===

    function set_slider_position(pos) {
        // Note: the left panel must be updated first. The left panel width is calculated there.
        // Updating the right panel relies on it.
        //
        // update left panel
        left_panel_width = pos
        resize_left_panel()
        // update right panel
        adjust_right_panel_width()
    }

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
        left_panel_width  = w_w - right_panel.width - PADDING_MIDDLE
        // "Canvas width=" + left_panel_width + " (based on window width " + w_w +
        // " and page panel width " + right_panel.width + ")"
    }

    function get_left_panel_size() {
        return {width: left_panel_width, height: panel_height}
    }

    function resize_left_panel() {
        left_panel.resize(get_left_panel_size())
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
        right_panel.width = w_w - left_panel_width - PADDING_MIDDLE
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
            panel_height = w_h - t_h - PADDING_BOTTOM
            // "Panel height=" + panel_height + " (based on window height " + w_h + " and toolbar height " + t_h + ")"
        }
    }



    // === Event Handling ===

    /**
     * Triggered repeatedly while the user moves the split pane's resizable-handle.
     */
    function do_resize(event, ui_event) {
        // Canvas resized: original with=" + ui_event.originalSize.width + " current with=" + ui_event.size.width
        set_slider_position(ui_event.size.width)
    }

    /**
     * Triggered while the user releases the split pane's resizable-handle.
     */
    function do_stop_resize() {
        // While resizing-via-handle jQuery UI adds a "style" attribute with absolute size values to the topicmap-panel.
        // This stops its flexible sizing (that follows the canvas element's size) and breaks the layout once the main
        // window is resized. Removing that style attribute once resizing-via-handle is finished solves that problem.
        $("#topicmap-panel").removeAttr("style")
        //
        left_panel.resize_end()
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
        resize_left_panel()
    }
}
