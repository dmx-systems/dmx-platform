function BooleanFieldRenderer(field_model) {
    this.field_model = field_model
}

BooleanFieldRenderer.prototype.render_field = function(parent_element) {
    dm4c.render.field_label(this.field_model, parent_element)
    parent_element.append(this.field_model.value.toString())
}

BooleanFieldRenderer.prototype.render_form_element = function(parent_element) {
    var form_element = dm4c.render.checkbox(this.field_model)    // a jQuery object
    parent_element.append(form_element)
    //
    return function() {
        return form_element.get(0).checked
    }
}
