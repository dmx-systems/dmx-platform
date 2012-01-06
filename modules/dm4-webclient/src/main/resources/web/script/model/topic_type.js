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

    // === "Page Displayable" implementation ===

    this.get_type = function() {
        return dm4c.type_cache.get_topic_type(this.type_uri)
    }

    this.get_commands = function(context) {
        return dm4c.get_topic_commands(this, context)
    }

    this.get_page_renderer_class = function() {
        return dm4c.get_view_config(this, "js_page_renderer_class") || "TopicRenderer"
    }

    // === Public API ===

    this.is_simple = function() {
        return !this.is_composite()
    }

    this.is_composite = function() {
        return this.data_type_uri == "dm4.core.composite"
    }

    // ---

    this.get_label_config = function(assoc_def_uri) {
        return js.contains(this.label_config, assoc_def_uri)
    }

    // --- View Configuration ---

    this.get_icon = function(type_uri) {
        return this.icon
    }

    this.load_icon = function() {
        this.icon = dm4c.create_image(this.get_icon_src())
    }

    /**
     * Returns the icon source.
     * If no icon is configured the source of the generic topic icon is returned.
     *
     * @return  The icon source (string).
     */
    this.get_icon_src = function() {
        return dm4c.get_view_config(this, "icon") || dm4c.DEFAULT_TOPIC_ICON
    }

    this.get_menu_config = function(menu_id) {
        switch (menu_id) {
        case "create-type-menu":
            return dm4c.get_view_config(this, "add_to_create_menu", true)
        case "search-type-menu":
            return true
        default:
            alert("TopicType.get_menu_config: menu \"" + menu_id + "\" not implemented")
        }
    }

    this.is_editable = function() {
        return dm4c.get_view_config(this, "editable", true)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function deserialize(assoc_defs) {
        for (var i = 0, assoc_def; assoc_def = assoc_defs[i]; i++) {
            assoc_def.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(assoc_def.view_config_topics))
        }
        return assoc_defs
    }
}
