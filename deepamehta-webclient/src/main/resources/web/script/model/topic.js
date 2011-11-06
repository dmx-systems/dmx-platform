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
     * Convenience method to lookup a simple value from this topic's direct composite.
     */
    get: function(key) {
        var topic = this.composite[key]
        return topic && topic.value
    },

    find_child_topic: function(type_uri) {

        return find_child_topic(this.composite)

        function find_child_topic(composite) {
            // alert("find_child_topic(): composite=" + JSON.stringify(composite))
            for (var assoc_def_uri in composite) {
                var child_topic = composite[assoc_def_uri]
                if (child_topic.type_uri == type_uri) {
                    return child_topic
                }
                child_topic = find_child_topic(child_topic.composite)
                if (child_topic) {
                    return child_topic
                }
            }
        }
    }
}
