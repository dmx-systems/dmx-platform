function RESTClient(core_service_uri) {

    var LOG_AJAX_REQUESTS = false

    // === Topics ===

    this.get_topic_by_id = function(topic_id) {
        return request("GET", "/topic/" + topic_id)
    }

    /**
     * Looks up a topic by exact property value.
     * If no such topic exists <code>null</code> is returned.
     * If more than one topic is found a runtime exception is thrown. FIXME: check this.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the property to be indexed with indexing mode <code>KEY</code>.
     *
     * @return  the topic, or <code>null</code>.
     */
    this.get_topic_by_property = function(key, value) {
        return request("GET", "/topic/by_property/" + encodeURIComponent(key) + "/" + encodeURIComponent(value))
    }

    this.get_topic = function(type_uri, key, value) {
        return request("GET", "/topic/" + encodeURIComponent(type_uri) + "/" +
            encodeURIComponent(key) + "/" + encodeURIComponent(value))
    }

    this.get_topics = function(type_uri) {
        return request("GET", "/topic/by_type/" + encodeURIComponent(type_uri))
    }

    /**
     * @param   include_topic_types     Optional: the topic type filter (array of topic type URIs, e.g.
     *                                  ["de/deepamehta/core/topictype/Note"]).
     *                                  If not specified (undefined or empty) the filter is switched off.
     *
     * @param   include_rel_types       Optional: the include relation type filter (array of strings of the form
     *                                  "<relTypeName>[;<direction>]", e.g. ["TOPICMAP_TOPIC;INCOMING"]).
     *                                  If not specified (undefined or empty) the filter is switched off.
     *
     * @param   exclude_rel_types       Optional: the exclude relation type filter (array of strings of the form
     *                                  "<relTypeName>[;<direction>]", e.g. ["SEARCH_RESULT;OUTGOING"]).
     *                                  If not specified (undefined or empty) the filter is switched off.
     *
     * @return  array of topics, possibly empty.
     */
    this.get_related_topics = function(topic_id, include_topic_types, include_rel_types, exclude_rel_types) {
        var params = new RequestParameter()
        params.add_list("include_topic_types", include_topic_types)
        params.add_list("include_rel_types", include_rel_types)
        params.add_list("exclude_rel_types", exclude_rel_types)
        return request("GET", "/topic/" + topic_id + "/related_topics" + params.to_query_string())
    }

    // FIXME: index parameter not used
    this.search_topics = function(index, text, field_uri, whole_word) {
        var params = new RequestParameter({search: text, field: field_uri, wholeword: whole_word})
        return request("GET", "/topic" + params.to_query_string())
    }

    this.create_topic = function(topic) {
        return request("POST", "/topic", topic)
    }

    this.set_topic_properties = function(topic_id, properties) {
        request("PUT", "/topic/" + topic_id, properties)
    }

    this.delete_topic = function(id) {
        request("DELETE", "/topic/" + id)
    }

    // === Relations ===

    /**
     * Returns the relation between two topics.
     * If no such relation exists null is returned. FIXME: check this.
     * If more than one relation exists, an exception is thrown. FIXME: check this.
     *
     * @param   type_id     Relation type filter (optional). Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter (optional). Pass <code>true</code> if direction matters. In this case the
     *                      relation is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     *
     * @return  The relation (a Relation object). FIXME: check this.
     */
    this.get_relation = function(src_topic_id, dst_topic_id, type_id, is_directed) {
        var params = new RequestParameter({src: src_topic_id, dst: dst_topic_id, type: type_id, directed: is_directed})
        return request("GET", "/relation" + params.to_query_string())
    }

    /**
     * Returns the relations between two topics.
     * If no such relation exists an empty array is returned.
     *
     * @param   type_id     Relation type filter (optional). Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter (optional). Pass <code>true</code> if direction matters. In this case the
     *                      relation is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     *
     * @return  An array of relations.
     */
    this.get_relations = function(src_topic_id, dst_topic_id, type_id, is_directed) {
        var params = new RequestParameter({src: src_topic_id, dst: dst_topic_id, type: type_id, directed: is_directed})
        return request("GET", "/relation/multiple" + params.to_query_string())
    }

    this.create_relation = function(relation) {
        return request("POST", "/relation", relation)
    }

    this.set_relation_properties = function(relation_id, properties) {
        request("PUT", "/relation/" + relation_id, properties)
    }

    this.delete_relation = function(id) {
        request("DELETE", "/relation/" + id)
    }

    // === Types ===

    this.get_topic_type_uris = function() {
        return request("GET", "/topictype")
    }

    this.get_topic_type = function(type_uri) {
        return request("GET", "/topictype/" + encodeURIComponent(type_uri))
    }

    this.create_topic_type = function(topic_type) {
        return request("POST", "/topictype", topic_type)
    }

    this.add_data_field = function(type_uri, field) {
        return request("POST", "/topictype/" + encodeURIComponent(type_uri), field)
    }

    this.update_data_field = function(type_uri, field) {
        return request("PUT", "/topictype/" + encodeURIComponent(type_uri), field)
    }

    this.set_data_field_order = function(type_uri, field_uris) {
        return request("PUT", "/topictype/" + encodeURIComponent(type_uri) + "/field_order", field_uris)
    }

    this.remove_data_field = function(type_uri, field_uri) {
        return request("DELETE", "/topictype/" + encodeURIComponent(type_uri) +
            "/field/" + encodeURIComponent(field_uri))
    }

    // === Commands ===

    this.execute_command = function(command, params) {
        return request("POST", "/command/" + encodeURIComponent(command), params)
    }

    // === Plugins ===

    this.get_plugins = function() {
        return request("GET", "/plugin")
    }

    /**
     * Sends an AJAX request. The URI is interpreted as an absolute URI.
     *
     * This utility method is called by plugins who register additional REST resources at an individual
     * namespace (server-side) and add corresponding service calls to the REST client instance.
     * For example, see the DeepaMehta 3 Topicmaps plugin.
     */
    this.request = function(method, uri, data) {
        return request(method, uri, data, true)
    }

    /**
     * This utility method is called by plugins who register additional REST resources.
     */
    this.createRequestParameter = function(params) {
        return new RequestParameter(params)
    }

    // === Private Helpers ===

    /**
     * Sends an AJAX request.
     *
     * @param   is_absolute_uri     If true, the URI is interpreted as relative to the DeepaMehta core service URI.
     *                              If false, the URI is interpreted as an absolute URI.
     */
    function request(method, uri, data, is_absolute_uri) {
        var status                  // "success" if request was successful
        var responseCode            // HTTP response code, e.g. 304
        var responseMessage         // HTTP response message, e.g. "Not Modified"
        var responseData            // in case of successful request: the response data (response body)
        var exception               // in case of unsuccessful request: possibly an exception
        //
        if (LOG_AJAX_REQUESTS) dm3c.log(method + " " + uri + "\n..... " + JSON.stringify(data))
        //
        $.ajax({
            type: method,
            url: is_absolute_uri ? uri : core_service_uri + uri,
            contentType: "application/json",
            data: JSON.stringify(data),
            processData: false,
            async: false,
            success: function(data, textStatus, xhr) {
                if (LOG_AJAX_REQUESTS) dm3c.log("..... " + xhr.status + " " + xhr.statusText +
                    "\n..... " + JSON.stringify(data))
                responseData = data
            },
            error: function(xhr, textStatus, ex) {
                if (LOG_AJAX_REQUESTS) dm3c.log("..... " + xhr.status + " " + xhr.statusText +
                    "\n..... exception: " + JSON.stringify(ex))
                exception = ex
            },
            complete: function(xhr, textStatus) {
                status = textStatus
                responseCode = xhr.status
                responseMessage = xhr.statusText
            }
        })
        if (status == "success") {
            return responseData
        } else {
            throw "AJAX request failed (" + responseCode + "): " + responseMessage + " (exception: " + exception + ")"
        }
    }

    function RequestParameter(params) {
        
        var param_array = []

        if (params && !params.length) {
            for (var param_name in params) {
                if (params[param_name]) {
                    add(param_name, params[param_name])
                }
            }
        }

        this.add = function(param_name, value) {
            add(param_name, value)
        }

        this.add_list = function(param_name, value_list) {
            if (value_list) {
                for (var i = 0; i < value_list.length; i++) {
                    add(param_name, value_list[i])
                }
            }
        }

        this.to_query_string = function() {
            var query_string = param_array.join("&")
            if (query_string) {
                query_string = "?" + query_string
            }
            return encodeURI(query_string)
        }

        function add(param_name, value) {
            param_array.push(param_name + "=" + value)
        }
    }
}
