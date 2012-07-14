dm4c.add_field_renderer("dm4.webclient.boolean_renderer", {

    render_field: function(field_model, parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value.toString())
    },

    render_form_element: function(field_model, parent_element) {
        var checkbox = dm4c.render.checkbox(field_model)    // a jQuery object
        parent_element.append(checkbox)
        //
        return function() {
            return checkbox.get(0).checked
        }
    }
})
