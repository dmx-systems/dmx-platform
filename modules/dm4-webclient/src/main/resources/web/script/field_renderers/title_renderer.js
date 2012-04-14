function TitleRenderer(field_model) {

    this.superclass = TextFieldRenderer
    this.superclass(field_model)

    this.render_field = function(parent_element) {
        parent_element.append($("<div>")
            .addClass("title")
            .text(js.render_text(field_model.value))
        )
    }
}
