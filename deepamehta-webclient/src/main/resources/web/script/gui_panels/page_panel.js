function PagePanel() {

    // Model
    var displayed_object = null     // a topic or an association

    // View
    var dom = $("<div>").attr("id", "page-panel")
    dom.append($("<div>").attr("id", "page-content"))
    dom.append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))
    show_splash()

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
        // set focus
        $("#page-content input, #page-content iframe").eq(0).focus()
        // FIXME: "iframe" is TinyMCE specific. Another WYSIWYG editor plugin might be in use.
        // FIXME: multiline plain text fields (<textarea>) should be considered too. Omitted for the
        // moment because a TinyMCE's textarea is hidden ("display: none") and can't be focused.
    }

    this.clear = function() {
        // update model
        displayed_object = null
        // update GUI
        empty(true)
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
        dm4c.trigger_page_renderer_hook(displayed_object, "render_page", displayed_object)
    }

    function render_form() {
        empty()
        dm4c.trigger_page_renderer_hook(displayed_object, "render_form", displayed_object)
    }

    function empty(do_show_splash) {
        $("#page-content").empty()
        $("#page-toolbar").empty()
        //
        if (do_show_splash) {
            show_splash()
        } else {
            hide_splash()
        }
    }

    // ---

    function render_buttons(context) {
        var commands = displayed_object.get_commands(context)
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            var button = dm4c.ui.button(undefined, cmd.handler, cmd.label, cmd.ui_icon, cmd.is_submit)
            $("#page-toolbar").append(button)
        }
    }

    // ---

    function show_splash() {
        $("#page-content").addClass("splash")
    }

    function hide_splash() {
        $("#page-content").removeClass("splash")
    }
}
