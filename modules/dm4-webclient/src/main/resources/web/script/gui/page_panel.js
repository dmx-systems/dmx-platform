function PagePanel() {

    var PageState = {
        PAGE: 1,
        FORM: 2,
        NONE: 3
    }

    // Model
    var displayed_object = null         // The object displayed in the page panel, or null if nothing is displayed
                                        // (a Topic object, or an Association object, or null).
    var page_renderer                   // The displayed object's page renderer
                                        // (only consulted if displayed_object is not null).
    var page_state = PageState.NONE     // Tracks page state. Used to fire the "post_destroy_form" event in case.
    var form_processing_function        // The form processing function: called to "submit" the form
                                        // (only consulted if a form is displayed).
    var form_processing_called          // Tracks the form processing function call (boolean).
                                        // Used to ensure the form processing function is called only once.

    // View
    var dom = $("<div>").attr("id", "page-panel")
        .append($("<div>").attr("id", "page-content").keyup(do_process_key))
        .append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))
    show_splash()

    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.render_page = function(topic_or_association) {
        // update model
        set_displayed_object(topic_or_association)
        // update view
        render_page()
    }

    this.render_page_if_selected = function(topic_or_association) {
        render_if_selected(topic_or_association, this.render_page)
    }

    // ---

    this.render_form = function(topic_or_association) {
        // update model
        set_displayed_object(topic_or_association)
        // update view
        render_form()
        // set focus
        $("#page-content input, #page-content iframe").eq(0).focus()
        // FIXME: "iframe" is TinyMCE specific. Another WYSIWYG editor plugin might be in use.
        // FIXME: multiline plain text fields (<textarea>) should be considered too. Omitted for the
        // moment because a TinyMCE's textarea is hidden ("display: none") and can't be focused.
        // ### Update: TinyMCE is replaced by CKEditor meanwhile.
    }

    this.render_form_if_selected = function(topic_or_association) {
        render_if_selected(topic_or_association, this.render_form)
    }

    // ---

    this.clear = function() {
        // update model
        set_displayed_object(null)
        // update view
        clear_page(PageState.NONE)
    }

    this.refresh = function() {
        // if page has been cleared before we perform nothing (rendering would fail)
        if (!displayed_object) {
            return
        }
        // update view
        render_page()
    }

    this.show_splash = function() {
        show_splash()
    }

    this.save = function() {
        if (page_state != PageState.FORM) {
            return
        }
        //
        if (form_processing_called) {
            throw "PagePanelError: the form processing function has already been called"
        }
        //
        form_processing_called = true
        form_processing_function()
    }

    this.get_displayed_object = function() {
        return displayed_object
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Model ===

    /**
     * @param   object      a topic or an association, or null if nothing is displayed.
     */
    function set_displayed_object(object) {
        displayed_object = object
        //
        if (object) {
            page_renderer = dm4c.get_page_renderer(object)
        }
    }

    // === View ===

    function render_page() {
        clear_page(PageState.PAGE)
        prepare_page()
        page_renderer.render_page(displayed_object)
        render_buttons("detail-panel-show")
    }

    function render_form() {
        clear_page(PageState.FORM)
        prepare_page()
        form_processing_function = page_renderer.render_form(displayed_object)
        render_buttons("detail-panel-edit")
        //
        form_processing_called = false
    }

    function render_buttons(context) {
        var commands = displayed_object.get_commands(context)
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            var button = dm4c.ui.button(cmd.handler, cmd.label, cmd.ui_icon, cmd.is_submit)
            $("#page-toolbar").append(button)
        }
    }

    function render_if_selected(topic_or_association, render_func) {
        if (displayed_object && displayed_object.id == topic_or_association.id) {
            render_func(topic_or_association)
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
            dm4c.fire_event("post_destroy_form")
        }
        // update model
        page_state = new_page_state
    }

    function prepare_page() {
        var css = page_renderer.page_css || {overflow: "auto"}
        $("#page-content").css(css)
    }

    // ---

    function show_splash() {
        $("#page-content").addClass("splash")
    }

    function hide_splash() {
        $("#page-content").removeClass("splash")
    }

    // === Events ===

    /**
     * Return key triggers submit button.
     */
    function do_process_key(event) {
        if (event.which == 13 && event.target.nodeName == "INPUT" && event.target.type == "text") {
            var submit_button = $("#page-toolbar button[type=submit]")
            // alert("do_process_key:\nsubmit button=\"" + submit_button.text() +
            //    "\"\ntarget.type=" + js.inspect(event.target.type))
            submit_button.click()
        }
        return false
    }
}
