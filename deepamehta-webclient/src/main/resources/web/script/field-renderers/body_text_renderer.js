function BodyTextRenderer(topic, field, rel_topics) {

    this.superclass = HTMLFieldRenderer
    this.superclass(topic, field, rel_topics)

    this.render_field = function(field_value_div) {
        // render field value
        return field.value
    }
}
