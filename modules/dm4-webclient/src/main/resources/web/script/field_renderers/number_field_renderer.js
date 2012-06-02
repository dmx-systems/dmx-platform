function NumberFieldRenderer(field_model) {
    this.field_model = field_model
}

NumberFieldRenderer.prototype.render_field = function(parent_element) {
    dm4c.render.field_label(this.field_model, parent_element)
    parent_element.append(this.field_model.value)
}

NumberFieldRenderer.prototype.render_form_element = function(parent_element) {
    var field_model = this.field_model      // needed in returned function
    var form_element = dm4c.render.input(field_model)
    parent_element.append(form_element)
    //
    return function() {
        var val = $.trim(form_element.val())
        var value = Number(val)
        if (isNaN(value)) {
            alert("WARNING: \"" + val + "\" is not a number.\n" +
                "(field \"" + field_model.label + "\")\n\n" +
                "The old value is restored.")
            return null                     // prevent this field from being updated
        }
        return value
    }
}
