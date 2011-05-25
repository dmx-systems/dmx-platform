function typeeditor_plugin() {

    dm3c.register_field_renderer("/de.deepamehta.3-typeeditor/script/field_definition_renderer.js")
    dm3c.css_stylesheet("/de.deepamehta.3-typeeditor/style/dm3-typeeditor.css")

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



    this.post_create_type_menu = function(type_menu) {
        type_menu.add_separator()
        type_menu.add_item({
            label: "New Topic Type...",
            value: "create_topic_type",
            is_trigger: true,
            handler: create_topic_type}
        )
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function create_topic_type() {
        var topic_type = dm3c.create_topic_type(DEFAULT_TOPIC_TYPE)
        dm3c.add_topic_to_canvas(topic_type, "edit")
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

}
