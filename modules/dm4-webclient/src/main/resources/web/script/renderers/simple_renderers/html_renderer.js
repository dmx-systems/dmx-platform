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
            autoGrow_onStartup: true,
            stylesSet: [
                /* Block Styles */
                {name: "Paragraph",   element: "p"},
                {name: "Heading 1",   element: "h1"},
                {name: "Heading 2",   element: "h2"},
                {name: "Heading 3",   element: "h3"},
                {name: "Heading 4",   element: "h4"},
                {name: "Code Block",  element: "pre",  attributes: {class: "code-block"}},
                {name: "Block Quote", element: "div",  attributes: {class: "blockquote"}},
                /* Inline Styles */
                {name: "Code",        element: "code"},
                {name: "Marker",      element: "span", attributes: {class: "marker"}}
            ]
        })
        //
        return function() {
            return editor.getData()
        }
    }
})
