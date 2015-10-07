function ToolbarPanel() {

    var TYPE_URI_NOTE = "dm4.notes.note"    // ### TODO: Notes should not be a Webclient dependency
    var CREATE_NOTE_BUTTON_TITLE = "Create a Note instantly [n]"

    var self = this

    // === Model ===

    var searchmodes = {}    // registry
    var searchmode_uri      // selected searchmode

    // === View ===

    // create "Search" widget
    var searchmode_menu = dm4c.ui.menu(do_select_searchmode)
    var searchmode_widget = $("<span>")
    var search_button = dm4c.ui.button({on_click: do_search, label: "Search", icon: "gear"})
    var search_widget = $("<div>").attr("id", "search-widget")
        .append(searchmode_menu.dom)
        .append(searchmode_widget)
        .append(search_button)
    // create "Create" widget
    var create_menu = dm4c.ui.menu(do_create_topic, "Create")
    var create_note_button = dm4c.ui.button({on_click: do_create_note, icon: "plus"})
        .attr({title: CREATE_NOTE_BUTTON_TITLE, accesskey: "n"})
    var create_widget = $("<div>").attr({id: "create-widget"})
        .append(create_menu.dom)
        .append(create_note_button)
    // create "Special" menu
    var special_menu = dm4c.ui.menu(undefined, "Help")  // renamed "Special" -> "Help" ### TODO: proper concept
    special_menu.dom.attr("id", "help-menu")
    // create toolbar
    var dom = $("<div>").attr({id: "main-toolbar"}).addClass("dm-toolbar")
        .append(search_widget)
        .append(create_widget)
        .append(special_menu.dom)

    // ----------------------------------------------------------------------------------------------- Public Properties

    this.searchmode_menu = searchmode_menu  // ### drop it?
    this.search_button = search_button      // ### drop it?
    this.create_widget = create_widget
    this.create_menu = create_menu
    this.special_menu = special_menu
    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Adds a searchmode to the searchmode menu.
     *
     * @param   searchmode_uri
     *              An unique URI to identify the searchmode (string).
     * @param   constructor_func
     *              The searchmode implementation (function).
     *              The function must define 3 properties:
     *                  "label" -- the searchmode's label as appearing in the searchmode menu (string).
     *                  "widget" -- the widget as displayed in the toolbar when the searchmode is selected (function).
     *                      The function receives no parameters and must return the widget's DOM (jQuery object,
     *                      DOM object, or HTML string). The function is called each time the searchmode is
     *                      selected, interactively or programmatically.
     *                  "search" -- the actual search process (function). The function receives no parameters and must
     *                      return an arbitrary topic, the "search bucket" to be shown on the topicmap panel. The
     *                      function is called each time the search is triggered, either interactively (by pressing
     *                      the toolbar's search button) or programmatically (by calling dm4c.toolbar.do_search()).
     */
    this.add_searchmode = function(searchmode_uri, constructor_func) {
        var searchmode = new constructor_func()
        searchmodes[searchmode_uri] = searchmode
        searchmode_menu.add_item({label: searchmode.label, value: searchmode_uri})
    }

    /**
     * Selects a searchmode programmatically.
     * This includes choosing the searchmode from the searchmode menu and displaying the searchmode's widget.
     *
     * @param   searchmode_uri  the URI of the searchmode to select. A searchmode with that URI must exist.
     */
    this.select_searchmode = function(_searchmode_uri) {
        // update model
        searchmode_uri = _searchmode_uri
        // update view
        searchmode_menu.select(searchmode_uri)
        show_searchmode_widget()
    }

    /**
     * Returns the URI of the selected searchmode.
     */
    this.get_searchmode_uri = function() {
        return searchmode_uri
    }

    /**
     * Triggers the current search as setup in the toolbar.
     * Like pressing the toolbar's search button programmatically.
     */
    this.do_search = function() {
        do_search()
    }

    /**
     * Enables/disables the toolbar's search button.
     *
     * @param   state   trueish to enable, falsish to disable.
     */
    this.enable_search_button = function(state) {
        search_button.button("option", "disabled", !state)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Search ===

    function do_select_searchmode(menu_item) {
        // update model
        searchmode_uri = menu_item.value
        // update view
        show_searchmode_widget()
    }

    function do_search() {
        var search_topic = new Topic(searchmodes[searchmode_uri].search())
        dm4c.page_panel.save()
        dm4c.show_topic(search_topic, "show", undefined, true)      // coordinates=undefined, do_center=true
    }

    // view update
    function show_searchmode_widget() {
        searchmode_widget.empty().append(searchmodes[searchmode_uri].widget())
    }

    // === Create ===

    function do_create_topic(menu_item) {
        dm4c.do_create_topic(menu_item.value)
    }

    function do_create_note() {
        dm4c.page_panel.save()
        dm4c.do_create_topic(TYPE_URI_NOTE)
    }
}
