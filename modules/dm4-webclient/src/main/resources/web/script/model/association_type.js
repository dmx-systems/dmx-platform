function AssociationType(assoc_type) {

    this.id        = assoc_type.id
    this.uri       = assoc_type.uri
    this.value     = assoc_type.value
    this.type_uri  = assoc_type.type_uri
    this.composite = assoc_type.composite
    //
    this.data_type_uri      = assoc_type.data_type_uri
    this.index_mode_uris    = assoc_type.index_mode_uris
    this.assoc_defs         = deserialize(assoc_type.assoc_defs)
    this.label_config       = assoc_type.label_config
    this.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(assoc_type.view_config_topics))
}

function deserialize(assoc_defs) {
    for (var i = 0, assoc_def; assoc_def = assoc_defs[i]; i++) {
        assoc_def.view_config_topics = dm4c.hash_by_type(dm4c.build_topics(assoc_def.view_config_topics))
    }
    return assoc_defs
}

// === "Page Displayable" implementation ===

AssociationType.prototype.get_type = function() {
    return dm4c.get_topic_type(this.type_uri)
}

AssociationType.prototype.get_commands = function(context) {
    return dm4c.get_topic_commands(this, context)
}

AssociationType.prototype.get_page_renderer_uri = function() {
    return dm4c.get_view_config(this, "page_renderer_uri")
}

// === Public API ===

AssociationType.prototype.is_simple = function() {
    return !this.is_composite()
}

AssociationType.prototype.is_composite = function() {
    return this.data_type_uri == "dm4.core.composite"
}

// --- View Configuration ---

AssociationType.prototype.get_color = function() {
    return dm4c.get_view_config(this, "color")
}
