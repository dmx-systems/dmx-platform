dm4c.add_plugin("de.deepamehta.typeeditor", function() {

    // Note: no "uri" is set here. A new topic type gets its default URI at server-side.
    var DEFAULT_TOPIC_TYPE = {
        value: "Topic Type Name",
        data_type_uri: "dm4.core.text",
        index_mode_uris: ["dm4.core.fulltext"],
        view_config_topics: [
            {
                type_uri: "dm4.webclient.view_config",
                childs: {
                    "dm4.webclient.show_in_create_menu": true
                }
            }
        ]
    }

    // Note: no "uri" is set here. A new association type gets its default URI at server-side.
    var DEFAULT_ASSOC_TYPE = {
        value: "Association Type Name",
        data_type_uri: "dm4.core.text",
        index_mode_uris: ["dm4.core.fulltext"],
        view_config_topics: [
            {
                type_uri: "dm4.webclient.view_config",
                childs: {
                    "dm4.webclient.show_in_create_menu": true    // ### TODO
                }
            }
        ]
    }

    // === Webclient Listeners ===

    /**
     * Once a topic type is created we refresh the "Create" type menu.
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
     * Once a topic type is updated we refresh the "Create" type menu.
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            dm4c.refresh_create_menu()
        }
    })

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        var tt = dm4c.has_create_permission("dm4.core.topic_type")
        var at = dm4c.has_create_permission("dm4.core.assoc_type")
        //
        if (tt || at) {
            type_menu.add_separator()
        }
        //
        if (tt) {
            type_menu.add_item({label: "New Topic Type", handler: function() {
                dm4c.do_create_topic_type(DEFAULT_TOPIC_TYPE)
            }})
        }
        if (at) {
            type_menu.add_item({label: "New Association Type", handler: function() {
                dm4c.do_create_association_type(DEFAULT_ASSOC_TYPE)
            }})
        }
    })
})
