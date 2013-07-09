function AssociationType(assoc_type) {
    Type.call(this, assoc_type)
}

AssociationType.prototype = new Type()

// === Public API ===

// --- View Configuration ---

AssociationType.prototype.get_color = function() {
    return dm4c.get_view_config(this, "color")
}
