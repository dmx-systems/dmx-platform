dm4c.add_simple_renderer("dm4.webclient.text_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var text = page_model.value
        // Note: the value might be a number (or something) if the type was Number before.
        // js.render_text() would fail.
        if (typeof text == "string") {
            text = js.render_text(text)
            if (page_model.input_field_rows > 1) {
                text = $("<p>").append(text)
            }
        }
        parent_element.append(text)
    },

    render_form: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var form_element = dm4c.render.form_element(page_model, parent_element)
        return dm4c.render.form_element_function(form_element, page_model)
    }
})
