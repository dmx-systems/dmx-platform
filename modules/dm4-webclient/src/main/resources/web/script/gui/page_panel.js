function PagePanel() {

    var PageState = {
        PAGE: 1,
        FORM: 2,
        NONE: 3
    }

    // Model
    var displayed_object = null         // a topic or an association, or null if nothing is displayed
    var page_state = PageState.NONE     // Tracks page state. Used to fire "post_destroy_form" in case.

    // View
    var dom = $("<div>").attr("id", "page-panel")
        .append($("<div>").attr("id", "page-content").keyup(do_process_key))
        .append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))
    show_splash()

    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.display = function(topic_or_association) {
        // update model
        displayed_object = topic_or_association
        // update GUI
        this.refresh()
    }

    this.display_conditionally = function(topic_or_association) {
        if (displayed_object.id == topic_or_association.id) {
            this.display(topic_or_association)
        }
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
        // ### Update: TinyMCE is replaced by CKEditor meanwhile.
    }

    this.clear = function() {
        // update model
        displayed_object = null
        // update GUI
        clear_page(PageState.NONE)
    }

    this.refresh = function() {
        // update GUI
        if (displayed_object) {  // if page has been cleared before we must not do anything (rendering would fail)
            render_page()
            render_buttons("detail-panel-show")
        }
    }

    this.show_splash = function() {
        show_splash()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Event Handler ===

    /**
     * Return key triggers submit button.
     */
    function do_process_key(event) {
        // dm4c.log("Key up " + event.which)
        if (event.which == 13) {
            var submit_button = $("#page-toolbar button[type=submit]")
            // alert("do_process_key: submit button=\"" + submit_button.text() + "\"")
            submit_button.click()
        }
        return false
    }

    // ===

    function render_page() {
        clear_page(PageState.PAGE)
        prepare_page()
        dm4c.trigger_page_renderer_hook(displayed_object, "render_page", displayed_object)
    }

    function render_form() {
        clear_page(PageState.FORM)
        prepare_page()
        dm4c.trigger_page_renderer_hook(displayed_object, "render_form", displayed_object)
    }

    function render_buttons(context) {
        var commands = displayed_object.get_commands(context)
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            var button = dm4c.ui.button(cmd.handler, cmd.label, cmd.ui_icon, cmd.is_submit)
            $("#page-toolbar").append(button)
        }
    }

    // ---

    function clear_page(new_page_state) {
        $("#page-content").empty()
        $("#page-toolbar").empty()
        //
        hide_splash()
        // fire event
        if (page_state == PageState.FORM) {
            dm4c.trigger_plugin_hook("post_destroy_form")
        }
        // update model
        page_state = new_page_state
    }

    function prepare_page() {
        var css = dm4c.trigger_page_renderer_hook(displayed_object, "page_css")
        $("#page-content").css(css)
    }

    // ---

    function show_splash() {
        $("#page-content").addClass("splash")
    }

    function hide_splash() {
        $("#page-content").removeClass("splash")
    }
}
