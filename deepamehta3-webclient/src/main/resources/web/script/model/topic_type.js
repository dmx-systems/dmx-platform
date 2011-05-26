function TopicType() {

    this.get_page_renderer_class = function() {
        return dm3c.get_view_config(this, "js_page_renderer_class") || "DefaultPageRenderer"
    }

    this.get_menu_config = function(menu_id) {
        switch (menu_id) {
        case "create-type-menu":
            return dm3c.get_view_config(this, "add_to_create_menu")
        case "search-type-menu":
            return true
        default:
            alert("TopicType.get_menu_config: menu \"" + menu_id + "\" not implemented")
        }
    }
}
