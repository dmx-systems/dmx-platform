dm4c.add_field_renderer("dm4.webclient.body_text_renderer", {

    render_field: function(field_model, parent_element) {
        parent_element.append(field_model.value)
    },

    render_form_element: dm4c.get_field_renderer("dm4.webclient.html_renderer").render_form_element
})
