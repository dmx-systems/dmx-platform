function typeeditor_plugin() {

    dm4c.register_page_renderer("/de.deepamehta.typeeditor/script/topictype_renderer.js")
    dm4c.register_css_stylesheet("/de.deepamehta.typeeditor/style/typeeditor.css")

    var DEFAULT_TOPIC_TYPE = {
        value: "Topic Type 1",
        uri: "domain.project.topic_type_1",
        data_type_uri: "dm4.core.text",
        index_mode_uris: ["dm4.core.fulltext"],
        view_config_topics: [
            {
                type_uri: "dm4.webclient.view_config",
                composite: {
                    "dm4.webclient.add_to_create_menu": true
                }
            }
        ]
    }

    // ------------------------------------------------------------------------------------------------------ Public API



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    /**
     * Once a topic type is created we must
     * 1) Update the type cache.
     * 2) Rebuild the "Create" button's type menu.
     *
     * @param   topic   The topic just created.
     *                  Note: in case the just created topic is a type, the entire type definition is passed.
     */
    this.post_create_topic = function(topic) {
        // ### FIXME: move code to dm4c.create_topic_type()?
        if (topic.type_uri == "dm4.core.topic_type") {
            // 1) Update type cache
            dm4c.type_cache.put_topic_type(topic)
            // 2) Rebuild type menu
            dm4c.refresh_create_menu()
        }
    }

    /**
     * Once a topic type is updated we must
     * 1) Update the type cache.
     * 2) Rebuild the "Create" button's type menu.
     */
    this.post_update_topic = function(topic, old_topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            // alert("post_update_topic:\n\nnew topic type=\n" + JSON.stringify(topic) +
            //     "\n\nold topic type=\n" + JSON.stringify(old_topic))
            // 1) Update type cache
            var uri_changed = topic.uri != old_topic.uri
            if (uri_changed) {
                // alert("Type URI changed: " + old_topic.uri + " -> " + topic.uri)
                dm4c.type_cache.remove(old_topic.uri)
            }
            dm4c.type_cache.put_topic_type(topic)
            // 2) Rebuild type menu
            dm4c.refresh_create_menu()
        }
    }

    this.post_refresh_create_menu = function(type_menu) {
        type_menu.add_separator()
        type_menu.add_item({
            label: "New Topic Type...",
            value: "create_topic_type",
            is_trigger: true,
            handler: function() {
                dm4c.do_create_topic_type(DEFAULT_TOPIC_TYPE)
            }
        })
    }
}
