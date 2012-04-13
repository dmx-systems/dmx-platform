function NumberFieldRenderer(field_model) {

    var gui_element

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field_model)
        // field value
        return field_model.value
    }

    this.render_form_element = function() {
        return gui_element = dm4c.render.input(field_model)
    }

    this.read_form_value = function() {
        var val = $.trim(gui_element.val())
        var content = Number(val)
        if (isNaN(content)) {
            alert("WARNING: \"" + val + "\" is not a number.\n" +
                "(field \"" + field_model.label + "\", path \"" + field_model.uri + "\")\n\n" +
                "The old value is restored.")
            return null     // prevent this field from being updated
        }
        return content
    }
}
