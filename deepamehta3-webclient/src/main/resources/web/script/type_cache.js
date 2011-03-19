function TypeCache() {

    var topic_types = {}        // key: Type URI, value: type definition (object with "uri", "fields", "label",
                                //                       "icon_src", and "js_renderer_class" attributes)
    var topic_type_icons = {}   // key: Type URI, value: icon (JavaScript Image object)

    // ------------------------------------------------------------------------------------------------------ Public API

    this.put = function(type_uri, topic_type) {
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

    this.get = function(type_uri) {
        return topic_types[type_uri]
    }

    /**
     * Looks up a type definition from the cache.
     *
     * @param   type_topic  the topic representing the type (object with "id", "type_uri", and "properties" attributes).
     *
     * @return  the type definition (object with "uri", "fields", "label", "icon_src", and "js_renderer_class"
     *          attributes).
     */
    this.get_topic_type = function(type_topic) {
        var type_uri = type_topic.properties["de/deepamehta/core/property/TypeURI"]
        var topic_type = topic_types[type_uri]
        //
        if (!topic_type) {
            throw "ERROR (get_topic_type): topic type not found in cache. Type URI: " + type_uri
        }
        //
        return topic_type
    }

    this.get_label = function(type_uri) {
        return topic_types[type_uri].label
    }

    this.get_icon = function(type_uri) {
        return topic_type_icons[type_uri]
    }

    /**
     * Returns an array of all type URIs in the cache.
     */
    this.get_type_uris = function() {
        return js.keys(topic_types)
    }

    // ---

    this.set_topic_type_uri = function(type_uri, new_type_uri) {
        var topic_type = topic_types[type_uri]  // lookup type
        this.remove(type_uri)                   // remove it from cache
        topic_type.uri = new_type_uri           // set new URI
        this.put(new_type_uri, topic_type)      // add to cache again
    }

    this.set_topic_type_label = function(type_uri, label) {
        topic_types[type_uri].label = label
    }

    // --- Data Fields ---

    this.get_data_field = function(topic_type, field_uri) {
        for (var i = 0, field; field = topic_type.fields[i]; i++) {
            if (field.uri == field_uri) {
                return field
            }
        }
    }

    this.add_field = function(type_uri, field) {
        topic_types[type_uri].fields.push(field)
    }

    this.remove_field = function(type_uri, field_uri) {
        var topic_type = topic_types[type_uri]
        var i = get_field_index(topic_type, field_uri)
        // error check 1
        if (i == undefined) {
            alert("ERROR (remove_field): field with URI \"" + field_uri +
                "\" not found in fields " + JSON.stringify(topic_type.fields))
            return
        }
        //
        topic_type.fields.splice(i, 1)
        // error check 2
        if (get_field_index(topic_type, field_uri) >= 0) {
            alert("ERROR (remove_field): more than one field with URI \"" + field_uri + "\" found")
            return
        }
    }

    // FIXME: rename to set_data_field_order once type cache is encapsulated in its own class
    this.update_data_field_order = function(type_uri, field_uris) {
        var topic_type = topic_types[type_uri]
        //
        var reordered_fields = []
        for (var i = 0, field_uri; field_uri = field_uris[i]; i++) {
            reordered_fields.push(this.get_data_field(topic_type, field_uri))
        }
        //
        if (topic_type.fields.length != reordered_fields.length) {
            throw "ERROR (update_data_field_order): There are " + topic_type.fields.length + " data fields " +
                "to order but " + reordered_fields.length + " has been reordered"
        }
        //
        topic_type.fields = reordered_fields
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_field_index(topic_type, field_uri) {
        for (var i = 0, field; field = topic_type.fields[i]; i++) {
            if (field.uri == field_uri) {
                return i
            }
        }
    }
}
