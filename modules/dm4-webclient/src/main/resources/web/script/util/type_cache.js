function TypeCache() {

    var self = this

    var topic_types = {}        // key: Type URI, value: a TopicType object
    var assoc_types = {}        // key: Type URI, value: a AssociationType object

    // ------------------------------------------------------------------------------------------------------ Public API

    this.get_topic_type = function(topic_type_uri) {
        var topic_type = topic_types[topic_type_uri]
        if (!topic_type) {
            throw "TypeCacheError: topic type \"" + topic_type_uri + "\" not found"
        }
        return topic_type
    }

    this.get_association_type = function(assoc_type_uri) {
        var assoc_type = assoc_types[assoc_type_uri]
        if (!assoc_type) {
            throw "TypeCacheError: association type \"" + assoc_type_uri + "\" not found"
        }
        return assoc_type
    }

    // ---

    this.put_topic_type = function(topic_type) {
        topic_types[topic_type.uri] = topic_type
    }

    this.put_association_type = function(assoc_type) {
        assoc_types[assoc_type.uri] = assoc_type
    }

    // ---

    this.remove_topic_type = function(topic_type_uri) {
        delete topic_types[topic_type_uri]
    }

    this.remove_association_type = function(assoc_type_uri) {
        delete assoc_types[assoc_type_uri]
    }

    // ---

    this.load_types = function(tracker) {
        load_topic_types(tracker)
        load_association_types(tracker)
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

    // ----------------------------------------------------------------------------------------------- Private Functions

    function load_topic_types(tracker) {
        dm4c.restc.get_all_topic_types(function(topic_types) {
            for (var i = 0, topic_type; topic_type = topic_types[i]; i++) {
                self.put_topic_type(new TopicType(topic_type))
            }
            // - Load type icons -
            // Note: the icons must be loaded *after* loading the topic types.
            // The topic type "dm4.webclient.icon" must be known.
            self.iterate(function(topic_type) {
                topic_type.load_icon()
            })
            //
            tracker.track()
        })
    }

    function load_association_types(tracker) {
        dm4c.restc.get_all_association_types(function(assoc_types) {
            for (var i = 0, assoc_type; assoc_type = assoc_types[i]; i++) {
                self.put_association_type(new AssociationType(assoc_type))
            }
            //
            tracker.track()
        })
    }

    // ---

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
