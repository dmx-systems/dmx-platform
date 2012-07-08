dm4c.add_field_renderer("dm4.webclient.title_renderer", {

    render_field: function(field_model, parent_element) {
        parent_element.append($("<h1>")
            .text(js.render_text(field_model.value))
        )
    },

    render_form_element: dm4c.get_field_renderer("dm4.webclient.text_renderer").render_form_element
})
