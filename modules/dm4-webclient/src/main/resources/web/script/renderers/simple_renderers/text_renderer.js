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
        return dm4c.render.form_element_function(render_form_element())

        /**
         * @return  an input field (a jQuery object), a text area (a jQuery object),
         *          or a combobox (a GUIToolkit Combobox object).
         */
        function render_form_element() {
            // error check
            // ### TODO: drop check. Meanwhile it's always set (default is 1). See dm4c.get_view_config()
            if (!page_model.input_field_rows) {
                throw "TextRendererError: field \"" + page_model.label + "\" has no \"input_field_rows\" setting"
            }
            //
            if (page_model.input_field_rows == 1) {
                return dm4c.render.form_element(page_model, parent_element)
            } else {
                return render_textarea()
            }

            function render_textarea() {
                var textarea = $("<textarea>").attr("rows", page_model.input_field_rows).text(page_model.value)
                render(textarea)
                return textarea
            }

            function render(form_element) {
                parent_element.append(form_element)
            }
        }
    }
})
