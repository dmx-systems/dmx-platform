dm4c.add_simple_renderer("dm4.webclient.title_renderer", {

    render_info: function(page_model, parent_element) {
        parent_element.append($("<h1>").text(page_model.value))
    },

    render_form: function(page_model, parent_element) {
        return dm4c.get_simple_renderer("dm4.webclient.text_renderer").render_form(page_model, parent_element)
    }
})
