function NumberFieldRenderer(field_model) {

    var gui_element

    this.render_field = function(parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value)
    }

    this.render_form_element = function(parent_element) {
        gui_element = dm4c.render.input(field_model)
        parent_element.append(gui_element)
    }

    this.read_form_value = function() {
        var val = $.trim(gui_element.val())
        var value = Number(val)
        if (isNaN(value)) {
            alert("WARNING: \"" + val + "\" is not a number.\n" +
                "(field \"" + field_model.label + "\")\n\n" +
                "The old value is restored.")
            return null     // prevent this field from being updated
        }
        return value
    }
}
