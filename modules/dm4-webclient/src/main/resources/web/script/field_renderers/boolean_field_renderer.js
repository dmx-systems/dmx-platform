function BooleanFieldRenderer(field_model) {

    this.render_field = function(parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value.toString())
    }

    this.render_form_element = function(parent_element) {
        var gui_element = dm4c.render.checkbox(field_model)     // a jQuery object
        parent_element.append(gui_element)
        //
        return function() {
            return gui_element.get(0).checked
        }
    }
}
