function TextFieldRenderer(topic, field) {

    /**
     * Input field: a jQuery object
     * Text area:   a jQuery object
     * Combo box:   a GUIToolkit Combobox object
     */
    var gui_element

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return js.render_text(field.value)
    }

    this.render_form_element = function() {
        if (!field.rows) {
            alert("WARNING (TextFieldRenderer.render_form_element):\n\nField \"" + field.uri +
                "\" has no \"rows\" setting.\n\nfield=" + JSON.stringify(field))
        } else if (field.rows == 1) {
            switch (field.assoc_def && field.assoc_def.assoc_type_uri) {
            case undefined:
                // Note: for non-composite topics the field's assoc_def is undefined.
                // We treat this like a composition here.
            case "dm4.core.composition_def":
                return gui_element = render_input()
            case "dm4.core.aggregation_def":
                gui_element = render_combobox()
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
            // gui_element is a Combobox
            var selection = gui_element.get_selection()   // either a menu item (object) or the text entered (string)
            if (typeof(selection) == "object") {
                // user selected existing topic
                return dm4c.REF_PREFIX + selection.value
            } else {
                // user entered new value
                return selection
            }
        }
    }

    // ---

    function render_input() {
        var input = dm4c.render.input(field)
        if (field.autocomplete_indexes) {
            var page_renderer = dm4c.get_page_renderer(topic)
            input.keyup(page_renderer.autocomplete)
            input.blur(page_renderer.lost_focus)
            input.attr({autocomplete: "off"})
        }
        return input
    }

    function render_textarea() {
        return $("<textarea>").attr("rows", field.rows).text(field.value)
    }

    function render_combobox() {

        // fetch all instances
        var topics = dm4c.restc.get_topics(field.topic_type.uri, false, true).items  // fetch_composite=false, sort=true
        return create_combobox();

        function create_combobox() {
            var combobox = dm4c.ui.combobox()
            // add items
            for (var i in topics) {
                combobox.add_item({label: topics[i].value, value: topics[i].id})
            }
            // select item
            combobox.select_by_label(field.value)
            //
            return combobox
        }
    }
}
