function TopicType(topic_type) {
    this.init(topic_type)
    this.icon = null    // The topic type's icon (JavaScript Image object).
                        // Note: it must be loaded *after* loading the topic types (see Webclient's load_types()).
}

TopicType.prototype = new Type()

// === Public API ===

// --- View Configuration ---

TopicType.prototype.get_icon = function() {
    return this.icon
}

TopicType.prototype.load_icon = function() {
    this.icon = dm4c.create_image(this.get_icon_src())
}

/**
 * Returns the icon source.
 * If no icon is configured the source of the generic topic icon is returned.
 *
 * @return  The icon source (string).
 */
TopicType.prototype.get_icon_src = function() {
    return dm4c.get_view_config(this, "icon")
}

TopicType.prototype.get_menu_config = function(menu_id) {
    switch (menu_id) {
    case "create-type-menu":
        return dm4c.get_view_config(this, "show_in_create_menu")
    case "search-type-menu":
        return true
    default:
        throw "TopicTypeError: \"" + menu_id + "\" is an unsupported menu ID"
    }
}

TopicType.prototype.is_hidden = function() {
    return dm4c.get_view_config(this, "hidden")
}

TopicType.prototype.is_locked = function() {
    return dm4c.get_view_config(this, "locked")
}
