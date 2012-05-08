function TextFieldRenderer(field_model) {

    this.render_field = function(parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(js.render_text(field_model.value))
    }

    this.render_form_element = function(parent_element) {
        var gui_element     // Input field: a jQuery object
                            // Text area:   a jQuery object
                            // Combo box:   a GUIToolkit Combobox object
        // error check
        if (!field_model.rows) {
            throw "TextFieldRendererError: field \"" + field_model.label + "\" has no \"rows\" setting"
        }
        //
        if (field_model.rows == 1) {
            switch (field_model.assoc_def && field_model.assoc_def.assoc_type_uri) {
            case undefined:
                // Note: for non-composite topics the field's assoc_def is undefined.
                // We treat this like a composition here.
            case "dm4.core.composition_def":
                gui_element = render_input()
                render(gui_element)
                break
            case "dm4.core.aggregation_def":
                gui_element = render_combobox()
                render(gui_element.dom)
                break
            default:
                throw "TextFieldRendererError: \"" + field_model.assoc_def.assoc_type_uri +
                    "\" is an unexpected assoc type URI"
            }
        } else {
            gui_element = render_textarea()
            render(gui_element)
        }
        //
        return function() {
            if (gui_element instanceof jQuery) {
                return $.trim(gui_element.val())
            } else {
                // gui_element is a Combobox
                var selection = gui_element.get_selection()  // either a menu item (object) or the text entered (string)
                if (typeof(selection) == "object") {
                    // user selected existing topic
                    return dm4c.REF_PREFIX + selection.value
                } else {
                    // user entered new value
                    return selection
                }
            }
        }

        function render(form_element) {
            parent_element.append(form_element)
        }
    }

    // ---

    function render_input() {
        var input = dm4c.render.input(field_model)
        if (field_model.autocomplete_indexes) {
            var page_renderer = dm4c.get_page_renderer(field_model.toplevel_topic)
            input.keyup(page_renderer.autocomplete)
            input.blur(page_renderer.lost_focus)
            input.attr({autocomplete: "off"})
        }
        return input
    }

    function render_textarea() {
        return $("<textarea>").attr("rows", field_model.rows).text(field_model.value)
    }

    function render_combobox() {
        // fetch all instances
        var topics = dm4c.restc.get_topics(field_model.topic_type.uri, false, true).items   // fetch_composite=false,
                                                                                            // sort=true
        return create_combobox();

        function create_combobox() {
            var combobox = dm4c.ui.combobox()
            // add items
            for (var i in topics) {
                combobox.add_item({label: topics[i].value, value: topics[i].id})
            }
            // select item
            combobox.select_by_label(field_model.value)
            //
            return combobox
        }
    }
}
