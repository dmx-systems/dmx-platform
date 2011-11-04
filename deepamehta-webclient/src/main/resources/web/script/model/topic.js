function Topic(topic) {
    
    this.id        = topic.id
    this.uri       = topic.uri
    this.value     = topic.value
    this.type_uri  = topic.type_uri
    this.composite = topic.composite

    // === "Page Displayable" implementation ===

    this.get_type = function() {
        return dm4c.type_cache.get_topic_type(this.type_uri)
    }

    this.get_commands = function(context) {
        return dm4c.get_topic_commands(this, context)
    }

    // === Public API ===

    /**
     * Convenience method to lookup a simple value from this topic's composite.
     */
    this.get = function(key) {
        var topic = this.composite[key]
        return topic && topic.value
    }
}
