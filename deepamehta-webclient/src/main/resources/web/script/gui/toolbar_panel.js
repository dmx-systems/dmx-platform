function ToolbarPanel() {

    var CREATE_ANOTHER_BUTTON_TITLE_DIS = "Once a topic is created this button creates another one of the same type [n]"
    var CREATE_ANOTHER_BUTTON_TITLE_EN  = "Create another "

    var recent_type_uri
    var self = this

    // create "Search" widget
    var searchmode_menu = dm4c.ui.menu(do_select_searchmode)
    var searchmode_widget = $("<span>")
    var search_button = dm4c.ui.button(do_search, "Search", "gear")
    var search_widget = $("<div>").attr("id", "search-widget")
        .append(searchmode_menu.dom)
        .append(searchmode_widget)
        .append(search_button)
    // create "Create" widget
    var create_menu = dm4c.ui.menu(do_create_topic, "Create")
    var create_another_button = dm4c.ui.button(do_create_another_topic, undefined, "plus")
        .button("disable").attr({title: CREATE_ANOTHER_BUTTON_TITLE_DIS, accesskey: "n"})
    var create_widget = $("<div>").attr({id: "create-widget"})
        .append(create_menu.dom)
        .append(create_another_button)
    // create "Special" menu
    var special_menu = dm4c.ui.menu(undefined, "Help")  // renamed "Special" -> "Help" ### TODO: proper concept
    // create toolbar
    var dom = $("<div>").attr({id: "upper-toolbar"}).addClass("dm-toolbar")
        .addClass("ui-widget-header").addClass("ui-corner-all")
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
        searchmode_widget.append(dm4c.trigger_plugin_hook("searchmode_widget", searchmode)[0])
    }

    this.refresh_create_menu = function() {
        // remove all items
        create_menu.empty()
        // add topic type items
        var type_uris = dm4c.type_cache.get_type_uris()
        for (var i = 0; i < type_uris.length; i++) {
            var type_uri = type_uris[i]
            var topic_type = dm4c.type_cache.get_topic_type(type_uri)
            if (dm4c.has_create_permission(type_uri) && topic_type.get_menu_config("create-type-menu")) {
                create_menu.add_item({
                    label: topic_type.value,
                    value: type_uri,
                    icon: topic_type.get_icon_src()
                })
            }
        }
    }

    this.get_recent_type_uri = function() {
        return recent_type_uri
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
        var type_uri = menu_item.value
        dm4c.do_create_topic(type_uri)
        // enable "create another one"
        recent_type_uri = type_uri
        var title = CREATE_ANOTHER_BUTTON_TITLE_EN + menu_item.label + " [n]"
        create_another_button.button("enable").attr("title", title)
    }

    function do_create_another_topic() {
        dm4c.do_create_topic(recent_type_uri)
    }

    // === Helper ===

    function get_searchmode(menu_item) {
        return menu_item.value
    }
}
