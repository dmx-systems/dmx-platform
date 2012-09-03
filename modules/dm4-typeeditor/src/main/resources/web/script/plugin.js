dm4c.add_plugin("de.deepamehta.typeeditor", function() {

    // Note: no "uri" is set here. A new topic type gets its default URI at server-side.
    var DEFAULT_TOPIC_TYPE = {
        value: "Topic Type Name",
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

    // === Webclient Listeners ===

    /**
     * Once a topic type is created we must refresh the "Create" type menu.
     *
     * @param   topic   The topic just created.
     *                  Note: in case the just created topic is a type, the entire type definition is passed.
     */
    dm4c.add_listener("post_create_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            dm4c.refresh_create_menu()
        }
    })

    /**
     * Once a topic type is updated we must refresh the "Create" type menu.
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            dm4c.refresh_create_menu()
        }
    })

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        if (!dm4c.has_create_permission("dm4.core.topic_type")) {
            return
        }
        //
        type_menu.add_separator()
        type_menu.add_item({label: "New Topic Type", handler: function() {
            dm4c.do_create_topic_type(DEFAULT_TOPIC_TYPE)
        }})
    })
})
