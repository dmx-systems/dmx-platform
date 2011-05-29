function Topic(topic) {
    
    this.id        = topic.id
    this.uri       = topic.uri
    this.value     = topic.value
    this.type_uri  = topic.type_uri
    this.composite = topic.composite

    this.get_type = function() {
        return dm3c.type_cache.get_topic_type(this.type_uri)
    }
}
