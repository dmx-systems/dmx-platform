function PagePanel() {

    // Model
    var displayed_object = null     // a topic or an association

    // View
    var dom = $("<div>").attr("id", "page-panel")
    dom.append($("<div>").attr("id", "page-content"))
    dom.append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))

    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.display = function(topic_or_association) {
        // update model
        displayed_object = topic_or_association
        // update GUI
        render_page()
    }

    this.edit = function(topic_or_association) {
        // update model
        displayed_object = topic_or_association
        // update GUI
        render_form()
    }

    this.clear = function() {
        // update model
        displayed_object = null
        // update GUI
        empty()
    }

    this.refresh = function() {
        // update GUI
        if (displayed_object) {  // if page has been cleared before we must not do anything (rendering would fail)
            render_page()
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_page() {
        empty()
        dm3c.trigger_page_renderer_hook(displayed_object, "render_page", displayed_object)
    }

    function render_form() {
        empty()
        dm3c.trigger_page_renderer_hook(displayed_object, "render_form", displayed_object)
    }

    function empty() {
        $("#page-content").empty()
        $("#page-toolbar").empty()
    }
}
