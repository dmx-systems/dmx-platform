function ToolbarPanel() {

    // the "Search" widget
    var searchmode_menu = dm4c.ui.menu("searchmode-select", do_select_searchmode)
    var search_button = dm4c.ui.button(undefined, do_search, "Search", "gear")
    var search_form = $("<form>").attr({action: "#", id: "search-form"})
        .append(searchmode_menu.dom)
        .append($("<span>").attr(                   {id: "search-widget"})
            .append($('<input type="text">').attr(  {id: "search-field", size: dm4c.SEARCH_FIELD_WIDTH})))
        .append(search_button).submit(do_search)
    // the "Create" widget
    var create_menu = dm4c.ui.menu("create-type-menu")
    var create_button = dm4c.ui.button(undefined, do_create_topic, "Create", "plus")
    var create_widget = $("<div>").attr(            {id: "create-widget"})
        .append(create_menu.dom)
        .append(create_button)
    // the "Special" menu
    var special_menu = dm4c.ui.menu("special-menu", undefined, undefined, "Special")
    var special_form = $("<div>").attr(             {id: "special-form"})
        .append(special_menu.dom)
    //
    var dom = $("<div>").attr({id: "upper-toolbar"}).addClass("dm-toolbar")
        .addClass("ui-widget-header").addClass("ui-corner-all")
        .append(search_form)
        .append(create_widget)
        .append(special_form)

    // ----------------------------------------------------------------------------------------------- Public Properties

    this.searchmode_menu = searchmode_menu
    this.create_menu = create_menu
    this.special_menu = special_menu
    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.refresh_create_menu = function() {
        // save selection and remove all items
        var selection = create_menu.get_selection()
        create_menu.empty()
        // add topic types
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
        // restore selection
        // Note: selection is undefined if the menu has no items.
        if (selection) {
            create_menu.select(selection.value)
        }
    }

    // ---

    function do_search() {
        var menu_item = searchmode_menu.get_selection()
        dm4c.do_search(get_searchmode(menu_item))
        return false    // suppress form submission
    }

    function do_select_searchmode(menu_item) {
        dm4c.do_select_searchmode(get_searchmode(menu_item))
    }

    // ---

    function do_create_topic() {
        var type_uri = create_menu.get_selection().value
        dm4c.do_create_topic(type_uri)
    }

    // ---

    function get_searchmode(menu_item) {
        return menu_item.value
    }
}
