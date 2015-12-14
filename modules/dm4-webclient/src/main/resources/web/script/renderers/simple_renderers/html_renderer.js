dm4c.add_simple_renderer("dm4.webclient.html_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(page_model.value)
    },

    render_form: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var textarea = $("<textarea>")
            .attr("rows", page_model.input_field_rows)
            .text(page_model.value)
        parent_element.append(textarea)
        var editor = CKEDITOR.replace(textarea.get(0), {
            contentsCss: "/de.deepamehta.webclient/css/ckeditor-contents.css",
            autoGrow_onStartup: true
        })
        //
        return function() {
            return editor.getData()
        }
    }
})
