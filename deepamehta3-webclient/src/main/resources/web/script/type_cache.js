function TypeCache() {

    var topic_types = {}        // key: Type URI, value: type definition
    var topic_type_icons = {}   // key: Type URI, value: icon (JavaScript Image object)
                                // FIXME: maintain icons in TopicType class

    // ------------------------------------------------------------------------------------------------------ Public API

    this.get = function(type_uri) {
        var topic_type = topic_types[type_uri]
        if (!topic_type) {
            throw "TopicTypeNotFound: topic type \"" + type_uri + "\" not found in type cache"
        }
        return topic_type
    }

    this.put = function(topic_type) {
        js.set_class(topic_type, TopicType)
        var type_uri = topic_type.uri
        topic_types[type_uri] = topic_type
        topic_type_icons[type_uri] = dm3c.create_image(dm3c.get_icon_src(type_uri))
    }

    this.remove = function(type_uri) {
        delete topic_types[type_uri]
        delete topic_type_icons[type_uri]
    }

    this.clear = function() {
        topic_types = {}
        topic_type_icons = {}
    }

    // ---

    // FIXME: move to TopicType class
    this.get_icon = function(type_uri) {
        return topic_type_icons[type_uri]
    }

    /**
     * Returns an array of all type URIs in the cache.
     */
    this.get_type_uris = function() {
        return js.keys(topic_types)
    }
}
