function PagePanel() {

    // Model
    var displayed_topic = null

    // View
    var dom = $("<div>").attr("id", "page-panel")
    dom.append($("<div>").attr("id", "page-content"))
    dom.append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))

    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.display = function(topic) {
        // update model
        displayed_topic = topic
        // update GUI
        render_page()
    }

    this.edit = function(topic) {
        // update model
        displayed_topic = topic
        // update GUI
        render_form()
    }

    this.clear = function() {
        // update model
        displayed_topic = null
        // update GUI
        empty()
    }

    this.refresh = function() {
        // update GUI
        render_page()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_page() {
        empty()
        dm3c.trigger_page_renderer_hook(displayed_topic, "render_page", displayed_topic)
    }

    function render_form() {
        empty()
        dm3c.trigger_page_renderer_hook(displayed_topic, "render_form", displayed_topic)
    }

    function empty() {
        $("#page-content").empty()
        $("#page-toolbar").empty()
    }
}
