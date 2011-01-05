function TimestampFieldRenderer(doc, field, rel_topics) {

    this.render_field = function() {
        // field label
        dm3c.render.field_label(field)
        // field value
        return js.format_timestamp(dm3c.get_value(doc, field.uri))
    }
}
