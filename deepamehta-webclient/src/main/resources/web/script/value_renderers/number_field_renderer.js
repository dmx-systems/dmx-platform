function NumberFieldRenderer(topic, field) {

    var gui_element

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return field.value
    }

    this.render_form_element = function() {
        return gui_element = dm4c.render.input(field)
    }

    this.read_form_value = function() {
        var val = $.trim(gui_element.val())
        var content = Number(val)
        if (isNaN(content)) {
            alert("WARNING: \"" + val + "\" is not a number.\n" +
                "(field \"" + field.label + "\", path \"" + field.uri + "\")\n\n" +
                "The old value is restored.")
            return null     // prevent this field from being updated
        }
        return content
    }
}
