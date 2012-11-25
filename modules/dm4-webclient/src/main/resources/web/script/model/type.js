function Type() {

    this.init = function(type) {
        this.id        = type.id
        this.uri       = type.uri
        this.value     = type.value
        this.type_uri  = type.type_uri
        this.composite = type.composite
        //
        this.data_type_uri      = type.data_type_uri
        this.index_mode_uris    = type.index_mode_uris
        this.assoc_defs         = deserialize(type.assoc_defs)
        this.label_config       = type.label_config
        this.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(type.view_config_topics))
    }

    // === "Page Displayable" implementation ===

    this.get_type = function() {
        return dm4c.get_topic_type(this.type_uri)
    }

    this.get_commands = function(context) {
        return dm4c.get_topic_commands(this, context)
    }

    this.get_page_renderer_uri = function() {
        return dm4c.get_view_config(this, "page_renderer_uri")
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

    // ------------------------------------------------------------------------------------------------- Private Methods

    function deserialize(assoc_defs) {
        for (var i = 0, assoc_def; assoc_def = assoc_defs[i]; i++) {
            assoc_def.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(assoc_def.view_config_topics))
        }
        return assoc_defs
    }
}
