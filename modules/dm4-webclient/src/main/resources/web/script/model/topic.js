function Topic(topic) {
    this.id        = topic.id
    this.uri       = topic.uri
    this.value     = topic.value
    this.type_uri  = topic.type_uri
    this.composite = topic.composite
}

Topic.prototype = {

    // === "Page Displayable" implementation ===

    get_type: function() {
        return dm4c.type_cache.get_topic_type(this.type_uri)
    },

    get_commands: function(context) {
        return dm4c.get_topic_commands(this, context)
    },

    // === Public API ===

    /**
     * Convenience method to lookup a simple value or a composite value.
     */
    get: function(key) {
        var comp = this.composite[key]
        if (comp) {
            var topic = new Topic(comp)
            if (topic.get_type().is_simple()) {
                return topic.value
            } else {
                return topic
            }
        }
    },

    find_child_topic: function(type_uri) {

        if (this.type_uri == type_uri) {
            return this
        }
        return find_child_topic(this.composite)

        function find_child_topic(composite) {
            // alert("find_child_topic(): composite=" + JSON.stringify(composite))
            for (var assoc_def_uri in composite) {
                var child_topic = composite[assoc_def_uri]
                // ### FIXME: should be if (child_topic.type_uri == type_uri)
                // ### but type_uri is not initialized in compact composite format
                if (assoc_def_uri == type_uri) {
                    return new Topic(child_topic)
                }
                child_topic = find_child_topic(child_topic.composite)
                if (child_topic) {
                    return new Topic(child_topic)
                }
            }
        }
    }
}
