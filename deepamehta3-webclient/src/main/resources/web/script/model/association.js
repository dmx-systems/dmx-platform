function Association(assoc) {
    
    this.id       = assoc.id
    this.type_uri = assoc.type_uri
    this.role_1   = assoc.role_1
    this.role_2   = assoc.role_2

    this.get_type = function() {
        return dm3c.type_cache.get_association_type(this.type_uri)
    }
}
