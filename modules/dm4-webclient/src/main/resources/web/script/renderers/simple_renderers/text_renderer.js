dm4c.add_simple_renderer("dm4.webclient.text_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var text = js.render_text(page_model.value)
        if (page_model.input_field_rows > 1) {
            text = $("<p>").append(text)
        }
        parent_element.append(text)
    },

    render_form: function(page_model, parent_element) {
        var form_element = dm4c.render.form_element(page_model, parent_element)
        return dm4c.render.form_element_function(form_element, page_model)
    }
})
