function TitleRenderer(field_model) {

    this.superclass = TextFieldRenderer
    this.superclass(field_model)

    this.render_field = function(field_value_div) {
        // render field value
        field_value_div.removeClass("field-value").addClass("title")
        return js.render_text(field_model.value)
    }
}
