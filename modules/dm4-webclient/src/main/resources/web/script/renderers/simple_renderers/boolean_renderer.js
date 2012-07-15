dm4c.add_simple_renderer("dm4.webclient.boolean_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(page_model.value.toString())
    },

    render_form: function(page_model, parent_element) {
        var checkbox = dm4c.render.checkbox(page_model)    // a jQuery object
        parent_element.append(checkbox)
        //
        return function() {
            return checkbox.get(0).checked
        }
    }
})
