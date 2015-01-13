dm4c.add_simple_renderer("dm4.webclient.number_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(page_model.value)
    },

    render_form: function(page_model, parent_element) {
        var form_element = dm4c.render.form_element(page_model, parent_element)
        return dm4c.render.form_element_function(form_element, page_model)
    }
})
