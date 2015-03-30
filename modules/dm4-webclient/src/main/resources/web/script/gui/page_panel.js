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
    var scroll_position = {}            // Per-object scrollbar positions (key: object ID, value: scrollTop value).
    var form_processing_function        // The form processing function: called to "submit" the form
                                        // (only consulted if a form is displayed).
    var form_processing_called          // Tracks the form processing function call (boolean).
                                        // Used to ensure the form processing function is called only once.

    // View
    var dom = $("<div>").attr("id", "page-panel")
        .append($("<div>").attr("id", "page-content").keydown(do_process_key))
        .append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))
    // Note: in conjunction with jQuery UI menu keyboard control "keydown" must be used here.
    // "keyup" would fire immediately once a "Create" menu item is selected via enter key.
    // This is because the "menuselect" event is fired early at "keydown" time. At time of
    // the subsequent "keyup" event the page panel form is already visible and focused.

    var page_state = PageState.NONE     // Tracks page state. Used to fire the "post_destroy_form" event in case.

    show_splash()

    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.render_page = function(topic_or_association) {
        // update model
        save_scroll_position()
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
        save_scroll_position()
        set_displayed_object(topic_or_association)
        // update view
        render_form()
        // set focus
        $("#page-content input, #page-content div[contenteditable=true]").eq(0).focus()
        // ### FIXME: focusing a CKEditor instance doesn't work this way (instance not yet ready?)
        // ### FIXME: multiline plain text fields (<textarea>) should be considered too. Note: for the
        // moment a CKEditor instance underlies a <textarea> too (display=none), see HTML Renderer.
    }

    this.render_form_if_selected = function(topic_or_association) {
        render_if_selected(topic_or_association, this.render_form)
    }

    // ---

    this.clear = function() {
        // update model
        save_scroll_position()
        set_displayed_object(null)
        // update view
        clear_page(PageState.NONE)
        // fire event
        var result = dm4c.fire_event("default_page_rendering")
        if (!js.contains(result, false)) {
            show_splash()
        }
    }

    this.clear_if_selected = function(topic_or_association) {
        render_if_selected(topic_or_association, this.clear)
    }

    // ---

    this.refresh = function() {
        // if page has been cleared before we perform nothing (rendering would fail)
        if (!displayed_object) {
            return
        }
        // update model
        save_scroll_position()
        // update view
        render_page()
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

    /**
     * @return  the object displayed in the page panel (a Topic object, or an Association object),
     *          or null if nothing is displayed.
     */
    this.get_displayed_object = function() {
        return displayed_object
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Model ===

    /**
     * @param   object      a Topic or an Association, or null if nothing is displayed.
     */
    function set_displayed_object(object) {
        displayed_object = object
        //
        if (object) {
            page_renderer = dm4c.get_page_renderer(object)
        }
    }

    // ---

    function save_scroll_position() {
        if (page_state == PageState.PAGE) {
            scroll_position[displayed_object.id] = $("#page-content").scrollTop()
        }
    }

    function restore_scroll_position() {
        var pos = scroll_position[displayed_object.id]
        if (pos != undefined) {
            $("#page-content").scrollTop(pos)
        }
    }

    // === View ===

    function render_page() {
        clear_page(PageState.PAGE)
        prepare_page()
        page_renderer.render_page(displayed_object)
        render_buttons("detail-panel-show")
        restore_scroll_position()
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
            var button = dm4c.ui.button({
                on_click:  cmd.handler,
                label:     cmd.label,
                icon:      cmd.ui_icon,
                is_submit: cmd.is_submit
            })
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
        //
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
            submit_button.click()
        }
        // Note: since we're bound to "keydown" (not "keyup") we must not prevent the default behavoir.
        // No letter would reach the input field.
    }
}
