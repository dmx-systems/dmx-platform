function TypeCache() {

    var self = this

    var topic_types = {}        // key: Type URI, value: a TopicType object
    var assoc_types = {}        // key: Type URI, value: a AssociationType object

    // ------------------------------------------------------------------------------------------------------ Public API

    this.get_topic_type = function(type_uri) {
        var topic_type = topic_types[type_uri]
        if (!topic_type) {
            throw "TypeCacheError: topic type \"" + type_uri + "\" not found"
        }
        return topic_type
    }

    this.get_association_type = function(type_uri) {
        var assoc_type = assoc_types[type_uri]
        if (!assoc_type) {
            throw "TypeCacheError: association type \"" + type_uri + "\" not found"
        }
        return assoc_type
    }

    // ---

    this.put_topic_type = function(topic_type) {
        var type_uri = topic_type.uri
        topic_types[type_uri] = topic_type
    }

    this.put_association_type = function(assoc_type) {
        var type_uri = assoc_type.uri
        assoc_types[type_uri] = assoc_type
    }

    // ---

    this.remove = function(type_uri) {
        delete topic_types[type_uri]
    }

    this.clear = function() {
        topic_types = {}
        assoc_types = {}
    }

    // ---

    this.iterate = function(visitor_func) {
        var type_uris = get_type_uris(true)     // sort=true
        for (var i = 0; i < type_uris.length; i++) {
            var topic_type = self.get_topic_type(type_uris[i])
            visitor_func(topic_type)
        }
    }

    /**
     * Returns an array of all type URIs in the cache.
     */
    function get_type_uris(sort) {
        var type_uris = js.keys(topic_types)
        // sort by type name
        if (sort) {
            type_uris.sort(function(uri_1, uri_2) {
                var name_1 = self.get_topic_type(uri_1).value
                var name_2 = self.get_topic_type(uri_2).value
                if (name_1 < name_2) {
                    return -1
                } else if (name_1 == name_2) {
                    return 0
                } else {
                    return 1
                }
            })
        }
        //
        return type_uris
    }
}
