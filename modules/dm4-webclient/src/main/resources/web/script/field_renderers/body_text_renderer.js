function BodyTextRenderer(field_model) {
    this.field_model = field_model
}

BodyTextRenderer.prototype = new HTMLFieldRenderer()

BodyTextRenderer.prototype.render_field = function(parent_element) {
    parent_element.append(this.field_model.value)
}
