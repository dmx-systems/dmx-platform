function TopicType(topic_type) {

    this.id        = topic_type.id
    this.uri       = topic_type.uri
    this.value     = topic_type.value
    this.type_uri  = topic_type.type_uri
    this.composite = topic_type.composite
    //
    this.data_type_uri      = topic_type.data_type_uri
    this.index_mode_uris    = topic_type.index_mode_uris
    this.assoc_defs         = deserialize(topic_type.assoc_defs)
    this.label_config       = topic_type.label_config
    this.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(topic_type.view_config_topics))
    //
    this.icon = null    // The topic type's icon (JavaScript Image object).
                        // Note: it must be loaded *after* loading the topic types (see Webclient's load_types()).
}

// ### TODO: create common base class (Type) for topic types and association types.
function deserialize(assoc_defs) {
    for (var i = 0, assoc_def; assoc_def = assoc_defs[i]; i++) {
        assoc_def.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(assoc_def.view_config_topics))
    }
    return assoc_defs
}

// === "Page Displayable" implementation ===

TopicType.prototype.get_type = function() {
    return dm4c.get_topic_type(this.type_uri)
}

TopicType.prototype.get_commands = function(context) {
    return dm4c.get_topic_commands(this, context)
}

TopicType.prototype.get_page_renderer_uri = function() {
    return dm4c.get_view_config(this, "page_renderer_uri")
}

// === Public API ===

TopicType.prototype.is_simple = function() {
    return !this.is_composite()
}

TopicType.prototype.is_composite = function() {
    return this.data_type_uri == "dm4.core.composite"
}

// ---

TopicType.prototype.get_label_config = function(assoc_def_uri) {
    return js.contains(this.label_config, assoc_def_uri)
}

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
        return dm4c.get_view_config(this, "add_to_create_menu")
    case "search-type-menu":
        return true
    default:
        alert("TopicType.get_menu_config: menu \"" + menu_id + "\" not implemented")
    }
}

TopicType.prototype.is_editable = function() {
    return dm4c.get_view_config(this, "editable")
}
