function TextFieldRenderer(topic, field, rel_topics) {

    /**
     * Input field: a jQuery object
     * Text area:   a jQuery object
     * Choice:      a UIHelper Menu object
     */
    var gui_element

    this.render_field = function() {
        // field label
        dm3c.render.field_label(field)
        // field value
        return js.render_text(field.value)
    }

    this.render_form_element = function() {
        if (!field.rows) {
            alert("WARNING (TextFieldRenderer.render_form_element):\n\nField \"" + field.uri +
                "\" has no \"rows\" setting.\n\nfield=" + JSON.stringify(field))
        } else if (field.rows == 1) {
            switch (field.assoc_def.assoc_type_uri) {
            case "dm3.core.composition":
                return gui_element = render_input()
            case "dm3.core.aggregation":
                gui_element = render_choice()
                return gui_element.dom
            default:
                alert("TextFieldRenderer.render_form_element(): unexpected assoc type URI (\"" +
                    field.assoc_def.assoc_type_uri + "\")")
            }
        } else {
            return gui_element = render_textarea()
        }
    }

    this.read_form_value = function() {
        if (gui_element instanceof jQuery) {
            return $.trim(gui_element.val())
        } else {
            var item = gui_element.get_selection()
            if (item) {
                return item.value
            } else {
                // alert("TextFieldRenderer: read_form_value() failed (" + field.topic_type.uri+ ")")
            }
        }
    }

    // ---

    function render_input() {
        var input = dm3c.render.input(field)
        if (field.autocomplete_indexes) {
            var doctype_impl = dm3c.get_doctype_impl(topic)
            input.keyup(doctype_impl.autocomplete)
            input.blur(doctype_impl.lost_focus)
            input.attr({autocomplete: "off"})
        }
        return input
    }

    function render_textarea() {
        return $("<textarea>").attr("rows", field.rows).text(field.value)
    }

    function render_choice() {

        // retrieve all instances
        var topics = dm3c.restc.get_topics(field.topic_type.uri)
        return create_choice();

        function create_choice() {
            //
            var menu = dm3c.ui.menu(field.uri)
            // ### menu.dom.addClass("field-editor-menu")
            // add items
            for (var i in topics) {
                menu.add_item({label: topics[i].value, value: topics[i].id})
            }
            // select item
            menu.select_by_label(field.value)
            //
            return menu
        }
    }
}
