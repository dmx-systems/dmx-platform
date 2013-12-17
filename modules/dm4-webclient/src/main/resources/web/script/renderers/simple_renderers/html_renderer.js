dm4c.add_simple_renderer("dm4.webclient.html_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(page_model.value)
    },

    render_form: function(page_model, parent_element) {
        parent_element.append($("<textarea>")
            .attr({id: "field_" + page_model.uri, rows: page_model.input_field_rows})
            .text(page_model.value)
        )
        CKEDITOR.inline("field_" + page_model.uri)
        //
        return function() {
            return CKEDITOR.instances["field_" + page_model.uri].getData()
        }
    }
})
