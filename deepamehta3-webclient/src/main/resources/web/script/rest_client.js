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
     * @param   assoc_type_uri  Association type filter (optional).
     *                          Pass <code>null</code>/<code>undefined</code> to switch filter off.
     *
     * @return  array of topics, possibly empty.
     */
    this.get_related_topics = function(topic_id, assoc_type_uri) {
        var params = new RequestParameter({assoc_type_uri: assoc_type_uri})
        return request("GET", "/topic/" + topic_id + "/related_topics?" + params.to_query_string())
    }

    // FIXME: index parameter not used
    this.search_topics = function(index, text, field_uri, whole_word) {
        var params = new RequestParameter({search: text, field: field_uri, wholeword: whole_word})
        return request("GET", "/topic?" + params.to_query_string())
    }

    this.create_topic = function(topic_data) {
        return request("POST", "/topic", topic_data)
    }

    this.update_topic = function(topic_data) {
        return request("PUT", "/topic", topic_data)
    }

    this.delete_topic = function(id) {
        request("DELETE", "/topic/" + id)
    }



    // === Associations ===

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
        return request("GET", "/relation?" + params.to_query_string())
    }

    /**
     * Returns the associations between two topics. If no such association exists an empty array is returned.
     *
     * @param   assoc_type_uri  Association type filter (optional).
     *                          Pass <code>null</code>/<code>undefined</code> to switch filter off.
     *
     * @return  An array of associations.
     */
    this.get_associations = function(topic1_id, topic2_id, assoc_type_uri) {
        return request("GET", "/association/multiple/" + topic1_id + "/" + topic2_id + "/" + (assoc_type_uri || ""))
    }

    this.create_association = function(assoc_data) {
        return request("POST", "/association", assoc_data)
    }

    this.set_relation_properties = function(relation_id, properties) {
        request("PUT", "/relation/" + relation_id, properties)
    }

    this.delete_association = function(id) {
        request("DELETE", "/association/" + id)
    }



    // === Types ===

    this.get_topic_type_uris = function() {
        return request("GET", "/topictype")
    }

    this.get_topic_type = function(type_uri) {
        return request("GET", "/topictype/" + encodeURIComponent(type_uri))
    }

    this.create_topic_type = function(properties, data_fields) {
        var params = new RequestParameter({properties: properties})
        params.add_list("data_fields", data_fields)
        return request("POST", "/topictype", params.to_query_string(), "application/x-www-form-urlencoded")
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



    // === Utilities for plugin developers ===

    /**
     * Sends an AJAX request. The URI is interpreted as an absolute URI.
     *
     * This utility method is called by plugins who register additional REST resources at an individual
     * namespace (server-side) and add corresponding service calls to the REST client instance.
     * For example, see the DeepaMehta 3 Topicmaps plugin.
     */
    this.request = function(method, uri, data, content_type) {
        return request(method, uri, data, content_type, true)
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
    function request(method, uri, data, content_type, is_absolute_uri) {
        var status                  // "success" if request was successful
        var responseCode            // HTTP response code, e.g. 304
        var responseMessage         // HTTP response message, e.g. "Not Modified"
        var responseData            // in case of successful request: the response data (response body)
        var exception               // in case of unsuccessful request: possibly an exception
        //
        if (LOG_AJAX_REQUESTS) dm3c.log(method + " " + uri + "\n..... " + JSON.stringify(data))
        //
        content_type = content_type || "application/json"       // set default
        if (content_type == "application/json") {
            data = JSON.stringify(data)
        }
        //
        $.ajax({
            type: method,
            url: is_absolute_uri ? uri : core_service_uri + uri,
            contentType: content_type,
            data: data,
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
            throw "AJAX " + method + " request failed, server response: " + responseCode +
                " (" + responseMessage + "), exception: " + exception
        }
    }

    function RequestParameter(params) {

        var param_array = []

        if (params && !params.length) {
            for (var param_name in params) {
                // do not add null or undefined values
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
            return encodeURI(param_array.join("&"))
        }

        function add(param_name, value) {
            if (typeof(value) == "object") {
                value = JSON.stringify(value)
            }
            param_array.push(param_name + "=" + value)
        }
    }
}
