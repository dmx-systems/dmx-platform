function FieldDefinitionRenderer(topic, field) {

    this.superclass = TextFieldRenderer
    this.superclass(topic, field)

    this.post_render_field = function() {
        // field label
        dm3c.render.field_label("URI")
        // field value
        dm3c.render.field_value(topic.uri)
    }

    this.post_render_form_element = function() {
        // field label
        dm3c.render.field_label("URI")
        // field value
        dm3c.render.field_value(dm3c.render.input(topic.uri))
    }
}
