function TimestampFieldRenderer(topic, field, rel_topics) {

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return js.format_timestamp(field.value)
    }
}
