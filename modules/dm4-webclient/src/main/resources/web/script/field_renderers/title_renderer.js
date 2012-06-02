function TitleRenderer(field_model) {
    this.field_model = field_model
}

TitleRenderer.prototype = new TextFieldRenderer()

TitleRenderer.prototype.render_field = function(parent_element) {
    parent_element.append($("<h1>")
        .text(js.render_text(this.field_model.value))
    )
}
