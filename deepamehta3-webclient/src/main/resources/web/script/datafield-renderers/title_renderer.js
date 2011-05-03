function TitleRenderer(topic, field, rel_topics) {

    this.superclass = TextFieldRenderer
    this.superclass(topic, field, rel_topics)

    this.render_field = function(field_value_div) {
        // render field value
        field_value_div.removeClass("field-value").addClass("title")
        return js.render_text(field.value)
    }
}
