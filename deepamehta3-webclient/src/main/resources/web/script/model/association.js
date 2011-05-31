function Association(assoc) {
    
    this.id       = assoc.id
    this.type_uri = assoc.type_uri
    this.role_1   = assoc.role_1
    this.role_2   = assoc.role_2

    // === "Page Displayable" implementation ===

    this.get_type = function() {
        return dm3c.type_cache.get_association_type(this.type_uri)
    }

    this.get_commands = function(context) {
        return dm3c.get_association_commands(this, context)
    }
}
