function BodyTextRenderer(field_model) {

    this.superclass = HTMLFieldRenderer
    this.superclass(field_model)

    this.render_field = function(parent_element) {
        parent_element.append(field_model.value)
    }
}
