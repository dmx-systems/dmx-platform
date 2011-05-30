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
        this.refresh()
    }

    this.edit = function(topic_or_association) {
        // update model
        displayed_object = topic_or_association
        // update GUI
        render_form()
        render_buttons("detail-panel-edit")
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
            render_buttons("detail-panel-show")
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

    // ---

    function render_buttons(context) {
        var commands = displayed_object.get_commands(context)
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            var button = dm3c.ui.button(undefined, cmd.handler, cmd.label, cmd.ui_icon, cmd.is_submit)
            $("#page-toolbar").append(button)
        }
    }
}
