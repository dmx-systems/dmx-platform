function BodyTextRenderer(field_model) {

    this.superclass = HTMLFieldRenderer
    this.superclass(field_model)

    this.render_field = function(field_value_div) {
        // render field value
        return field_model.value
    }
}
