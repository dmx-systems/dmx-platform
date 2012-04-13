function BooleanFieldRenderer(field_model) {

    /**
     * Checkbox: a jQuery object
     */
    var gui_element

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field_model)
        // field value
        return field_model.value.toString()
    }

    this.render_form_element = function() {
        return gui_element = dm4c.render.checkbox(field_model)
    }

    this.read_form_value = function() {
        return gui_element.get(0).checked
    }
}
