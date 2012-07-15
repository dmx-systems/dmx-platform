dm4c.add_simple_renderer("dm4.webclient.body_text_renderer", {

    render_info: function(page_model, parent_element) {
        parent_element.append(page_model.value)
    },

    render_form: dm4c.get_simple_renderer("dm4.webclient.html_renderer").render_form
})
