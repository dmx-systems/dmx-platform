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
    this.view_config_topics = dm4c.hash_by_type(topic_type.view_config_topics)

    // === "Page Displayable" implementation ===

    this.get_type = function() {
        return dm4c.type_cache.get_topic_type(this.type_uri)
    }

    this.get_commands = function(context) {
        return dm4c.get_topic_commands(this, context)
    }

    this.get_page_renderer_class = function() {
        return dm4c.get_view_config(this, "js_page_renderer_class")
    }

    // === "View Configurable" implementation ===

    this.default_page_renderer_class = function() {
        return "TopicRenderer"
    }

    // === Public API ===

    this.is_editable = function() {
        return dm4c.get_view_config(this, "editable")
    }

    /**
     * Returns the icon source.
     * If no icon is configured the source of the generic topic icon is returned.
     *
     * @return  The icon source (string).
     */
    this.get_icon_src = function() {
        return dm4c.get_view_config(this, "icon")
    }

    this.get_menu_config = function(menu_id) {
        switch (menu_id) {
        case "create-type-menu":
            return dm4c.get_view_config(this, "add_to_create_menu")
        case "search-type-menu":
            return true
        default:
            alert("TopicType.get_menu_config: menu \"" + menu_id + "\" not implemented")
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function deserialize(assoc_defs) {
        for (var i = 0, assoc_def; assoc_def = assoc_defs[i]; i++) {
            assoc_def.view_config_topics = dm4c.hash_by_type(assoc_def.view_config_topics)
        }
        return assoc_defs
    }
}
