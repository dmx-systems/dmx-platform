function ToolbarPanel() {

    var TYPE_URI_NOTE = "dm4.notes.note"    // ### TODO: Notes should not be a Webclient dependency
    var CREATE_NOTE_BUTTON_TITLE = "Create a Note instantly [n]"

    var self = this

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

    this.searchmode_menu = searchmode_menu
    this.search_button = search_button
    this.create_widget = create_widget
    this.create_menu = create_menu
    this.special_menu = special_menu
    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.select_searchmode = function(searchmode) {
        searchmode_widget.empty()
        searchmode_widget.append(dm4c.fire_event("searchmode_widget", searchmode)[0])
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Event Handler ===

    function do_search() {
        var searchmode = get_searchmode(searchmode_menu.get_selection())
        dm4c.do_search(searchmode)
    }

    function do_select_searchmode(menu_item) {
        self.select_searchmode(get_searchmode(menu_item))
    }

    // ---

    function do_create_topic(menu_item) {
        dm4c.do_create_topic(menu_item.value)
    }

    function do_create_note() {
        dm4c.do_create_topic(TYPE_URI_NOTE)
    }

    // === Helper ===

    function get_searchmode(menu_item) {
        return menu_item.value
    }
}
