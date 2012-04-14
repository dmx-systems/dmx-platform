function BooleanFieldRenderer(field_model) {

    /**
     * Checkbox: a jQuery object
     */
    var gui_element

    this.render_field = function(parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value.toString())
    }

    this.render_form_element = function(parent_element) {
        gui_element = dm4c.render.checkbox(field_model)
        parent_element.append(gui_element)
    }

    this.read_form_value = function() {
        return gui_element.get(0).checked
    }
}
