function Association(assoc) {
    this.id       = assoc.id
    this.uri      = assoc.uri
    this.type_uri = assoc.type_uri
    this.value    = assoc.value
    this.childs   = build_child_topics(assoc.childs)   // build_child_topics is defined in topic.js
    this.role_1   = assoc.role_1
    this.role_2   = assoc.role_2
    this.topic_1  = null
    this.topic_2  = null
}

// === "Page Displayable" implementation ===

Association.prototype.get_type = function() {
    return dm4c.get_association_type(this.type_uri)
}

Association.prototype.get_commands = function(context) {
    return dm4c.get_association_commands(this, context)
}

// === Public API ===

Association.prototype.get_topic_1 = function() {
    if (!this.topic_1) {
        this.topic_1 = dm4c.fetch_topic(this.role_1.topic_id)
    }
    return this.topic_1
}

Association.prototype.get_topic_2 = function() {
    if (!this.topic_2) {
        this.topic_2 = dm4c.fetch_topic(this.role_2.topic_id)
    }
    return this.topic_2
}

// ---

Association.prototype.get_role_type_1 = function() {
    return dm4c.restc.get_topic_by_value("uri", this.role_1.role_type_uri)
}

Association.prototype.get_role_type_2 = function() {
    return dm4c.restc.get_topic_by_value("uri", this.role_2.role_type_uri)
}

// ---

Association.prototype.get_topic_by_type = function(topic_type_uri) {
    var topic_1 = this.get_topic_1()
    var topic_2 = this.get_topic_2()
    var match_1 = topic_1.type_uri == topic_type_uri
    var match_2 = topic_2.type_uri == topic_type_uri
    if (match_1 && match_2) {
        throw "AssociationError: ambiguity in association " + this.id +
            " when looking for the \"" + topic_type_uri + "\" topic"
    }
    return match_1 ? topic_1 : match_2 ? topic_2 : undefined
}

Association.prototype.get_role = function(role_type_uri) {
    var match_1 = this.role_1.role_type_uri == role_type_uri
    var match_2 = this.role_2.role_type_uri == role_type_uri
    if (match_1 && match_2) {
        throw "AssociationError: ambiguity in association " + this.id +
            " when looking for the \"" + role_type_uri + "\" role"
    }
    return match_1 ? this.role_1 : match_2 ? this.role_2 : undefined
}

Association.prototype.matches = function(type_uri_1, type_uri_2) {
    var topic_1 = this.get_topic_1()
    var topic_2 = this.get_topic_2()
    return topic_1.type_uri == type_uri_1 && topic_2.type_uri == type_uri_2 ||
           topic_1.type_uri == type_uri_2 && topic_2.type_uri == type_uri_1
}
