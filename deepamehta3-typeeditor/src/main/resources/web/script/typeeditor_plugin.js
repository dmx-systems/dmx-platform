function typeeditor_plugin() {

    dm3c.register_field_renderer("/de.deepamehta.3-typeeditor/script/field_definition_renderer.js")
    dm3c.css_stylesheet("/de.deepamehta.3-typeeditor/style/dm3-typeeditor.css")

    // ------------------------------------------------------------------------------------------------------ Public API



    // ************************************************************
    // *** Webclient Hooks (triggered by deepamehta3-webclient) ***
    // ************************************************************



    this.custom_create_topic = function(type_uri) {
    }

    this.post_create_topic = function(topic) {
    }

    this.post_update_topic = function(topic, old_properties) {
    }

    this.post_delete_topic = function(topic) {
    }

    this.pre_render_form = function(topic) {
    }

    this.pre_submit_form = function(topic) {
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    // ------------------------------------------------------------------------------------------------- Private Classes

}
