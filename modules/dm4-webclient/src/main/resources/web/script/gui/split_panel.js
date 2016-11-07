function SplitPanel() {

    var PAGE_TOOLBAR_HEIGHT = 60

    var topicmap_renderer_parent = $("<td>").append($("<div>", {id: "topicmap-panel"}))
    var page_panel_parent        = $("<td>")

    var topicmap_renderer               // the left panel
    var page_panel                      // the right panel (added first)

    var page_panel_space                // horizontal margin + padding

    var topicmap_renderer_width
    var topicmap_renderer_height

    var topicmap_renderer_uris = []     // keeps track of topicmap renderer initialization

    this.dom = $("<table>", {id: "split-panel"}).append($("<tr>")
        .append(topicmap_renderer_parent)
        .append(page_panel_parent)
    )

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @param   _topicmap_renderer  By default the CanvasRenderer instance.
     *                              An object with a "dom" property and "get_info", "init", "resize", "resize_end"
     *                              methods.
     */
    this.set_topicmap_renderer = function(_topicmap_renderer) {
        topicmap_renderer = _topicmap_renderer
        //
        // 1) add topicmap renderer to page (by replacing the existing one)
        // Note: class "topicmap-renderer" is added in order to address the topicmap renderer for removal.
        $("#topicmap-panel .topicmap-renderer").remove()
        $("#topicmap-panel").append(_topicmap_renderer.dom.addClass("topicmap-renderer"))
        // Note: re-added topicmap renderers must be resized to adapt to possibly changed slider position.
        resize_topicmap_renderer()
        // Note: resizing takes place *after* replacing. This allows topicmap renderers to recreate their DOM
        // to realize resizing. Otherwise the topicmap renderer's event handlers would be lost while replacing.
        //
        // 2) init topicmap renderer
        var topicmap_renderer_uri = _topicmap_renderer.get_info().uri
        // Note: each topicmap renderer is initialized only once. We recognize topicmap renderers by their URIs.
        if (!js.contains(topicmap_renderer_uris, topicmap_renderer_uri)) {
            topicmap_renderer_uris.push(topicmap_renderer_uri)
            _topicmap_renderer.init()
        }
    }

    /**
     * @param   _page_panel     The PagePanel instance.
     *                          An object with a "dom" property.
     */
    this.set_page_panel = function(_page_panel) {
        page_panel = _page_panel
        page_panel_parent.append(_page_panel.dom)
        //
        adjust_page_panel_height()
        page_panel.width = $("#page-content").width()
        page_panel_space = _page_panel.dom.outerWidth(true) - page_panel.width  // includeMargin=true
        //
        calculate_topicmap_renderer_size()
        $("#topicmap-panel").resizable({handles: "e", resize: do_resize, stop: do_stop_resize})
        //
        $(window).resize(do_window_resize)
    }

    // ---

    this.get_slider_position = function() {
        return topicmap_renderer_width
    }

    this.set_slider_position = function(pos) {
        set_slider_position(pos)
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Size Management ===

    function set_slider_position(pos) {
        // Note: the left panel must be updated first. The left panel width is calculated there.
        // Updating the right panel relies on it.
        //
        // left panel
        topicmap_renderer_width = pos
        resize_topicmap_renderer()
        // right panel
        adjust_page_panel_width()
    }

    // --- Left Panel ---

    /**
     * Calculates the topicmap renderer's size based on window width and page panel's width.
     * Stores the result in "topicmap_renderer_width" and "topicmap_renderer_height".
     *
     * Called in 2 situations:
     *     1) initial setup
     *     2) the browser window is resized
     */
    function calculate_topicmap_renderer_size() {
        // update model
        topicmap_renderer_width  = window.innerWidth  - page_panel.width - page_panel_space
        topicmap_renderer_height = window.innerHeight - dm4c.toolbar.dom.outerHeight()
    }

    function resize_topicmap_renderer() {
        // update view
        topicmap_renderer.resize({
            width:  topicmap_renderer_width,
            height: topicmap_renderer_height
        })
    }

    // --- Right Panel ---

    /**
     * Calculates the right panel's width based on window width and topicmap renderer's width.
     *
     * Called in 1 situation: the resizable-handle is moved.
     */
    function adjust_page_panel_width() {
        // update model
        page_panel.width = window.innerWidth - topicmap_renderer_width - page_panel_space
        // update view
        $("#page-content").width(page_panel.width)
    }

    /**
     * Called in 2 situations:
     *     1) initial setup
     *     2) the browser window is resized
     */
    function adjust_page_panel_height() {
        // update model
        var page_content_height = window.innerHeight - dm4c.toolbar.dom.outerHeight() - PAGE_TOOLBAR_HEIGHT
        page_panel.height = page_content_height
        // update view
        $("#page-content").height(page_content_height)
    }



    // === Event Handling ===

    /**
     * Triggered repeatedly while the user moves the split pane's resizable-handle.
     */
    function do_resize(event, ui_event) {
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
        topicmap_renderer.resize_end()
    }

    // ---

    /**
     * Triggered repeatedly while the user resizes tge browser window.
     */
    function do_window_resize() {
        // Note: the right panel must be updated first. The panel height is calculated there.
        // Updating the left panel relies on it.
        //
        // right panel
        adjust_page_panel_height()
        // left panel
        calculate_topicmap_renderer_size()
        resize_topicmap_renderer()
    }
}
