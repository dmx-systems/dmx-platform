function typeeditor_plugin() {

    dm4c.register_page_renderer("/de.deepamehta.typeeditor/script/topictype_renderer.js")
    dm4c.register_css_stylesheet("/de.deepamehta.typeeditor/style/typeeditor.css")

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

    // ------------------------------------------------------------------------------------------------------ Public API



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    /**
     * Once a topic type is created we must refresh the "Create" type menu.
     *
     * @param   topic   The topic just created.
     *                  Note: in case the just created topic is a type, the entire type definition is passed.
     */
    this.post_create_topic = function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            dm4c.refresh_create_menu()
        }
    }

    /**
     * Once a topic type is updated we must refresh the "Create" type menu.
     */
    this.post_update_topic = function(topic, old_topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
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
