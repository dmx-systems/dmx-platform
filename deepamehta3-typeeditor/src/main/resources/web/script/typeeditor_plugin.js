function typeeditor_plugin() {

    dm3c.register_page_renderer("/de.deepamehta.3-typeeditor/script/topictype_renderer.js")
    dm3c.register_css_stylesheet("/de.deepamehta.3-typeeditor/style/dm3-typeeditor.css")

    var DEFAULT_TOPIC_TYPE = {
        value: "Topic Type 1",
        uri: "domain.project.topic_type_1",
        data_type_uri: "dm3.core.text",
        index_mode_uris: ["dm3.core.fulltext"],
        view_config_topics: [
            {
                type_uri: "dm3.webclient.view_config",
                composite: {
                    "dm3.webclient.add_to_create_menu": true
                }
            }
        ]
    }

    // ------------------------------------------------------------------------------------------------------ Public API



    // ************************************************************
    // *** Webclient Hooks (triggered by deepamehta3-webclient) ***
    // ************************************************************



    /**
     * Once a topic type is created we must
     * 1) Update the type cache.
     * 2) Rebuild the "Create" button's type menu.
     *
     * @param   topic   The topic just created.
     *                  Note: in case the just created topic is a type, the entire type definition is passed.
     */
    this.post_create_topic = function(topic) {
        if (topic.type_uri == "dm3.core.topic_type") {
            // 1) Update type cache
            dm3c.type_cache.put_topic_type(topic)
            // 2) Rebuild type menu
            dm3c.recreate_type_menu("create-type-menu")
        }
    }

    /**
     * Once a topic type is updated we must
     * 1) Update the type cache.
     * 2) Rebuild the "Create" button's type menu.
     */
    this.post_update_topic = function(topic, old_topic) {
        if (topic.type_uri == "dm3.core.topic_type") {
            // alert("post_update_topic:\n\nnew topic type=\n" + JSON.stringify(topic) +
            //     "\n\nold topic type=\n" + JSON.stringify(old_topic))
            // 1) Update type cache
            var uri_changed = topic.uri != old_topic.uri
            var value_changed = topic.value != old_topic.value
            if (uri_changed) {
                // alert("Type URI changed: " + old_topic.uri + " -> " + topic.uri)
                dm3c.type_cache.remove(old_topic.uri)
                dm3c.type_cache.put_topic_type(topic)
            }
            if (value_changed) {
                // alert("Type name changed: " + old_topic.value + " -> " + topic.value)
                dm3c.type_cache.get_topic_type(topic.uri).value = topic.value
            }
            // 2) Rebuild type menu
            if (uri_changed || value_changed) {
                dm3c.recreate_type_menu("create-type-menu")
            }
        }
    }

    this.post_create_type_menu = function(type_menu) {
        type_menu.add_separator()
        type_menu.add_item({
            label: "New Topic Type...",
            value: "create_topic_type",
            is_trigger: true,
            handler: create_topic_type
        })
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function create_topic_type() {
        var topic_type = dm3c.create_topic_type(DEFAULT_TOPIC_TYPE)
        dm3c.add_topic_to_canvas(topic_type, "edit")
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

}
