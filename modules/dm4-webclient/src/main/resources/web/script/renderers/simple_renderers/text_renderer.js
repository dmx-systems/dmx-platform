dm4c.add_simple_renderer("dm4.webclient.text_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var text = js.render_text(page_model.value)
        if (page_model.rows > 1) {
            text = $("<p>").append(text)
        }
        parent_element.append(text)
    },

    render_form: function(page_model, parent_element) {
        // Input field: a jQuery object
        // Text area:   a jQuery object
        // Combo box:   a GUIToolkit Combobox object
        var form_element = render_form_element()
        //
        return function() {
            if (form_element instanceof jQuery) {
                return $.trim(form_element.val())
            } else {
                // form_element is a Combobox
                var selection = form_element.get_selection() // either a menu item (object) or the text entered (string)
                if (typeof(selection) == "object") {
                    // user selected existing topic
                    return dm4c.REF_PREFIX + selection.value
                } else {
                    // user entered new value
                    return selection
                }
            }
        }

        // ------------------------------------------------------------------------------------------- Private Functions

        function render_form_element() {
            // error check
            if (!page_model.rows) {
                throw "TextRendererError: field \"" + page_model.label + "\" has no \"rows\" setting"
            }
            //
            if (page_model.rows == 1) {
                switch (page_model.assoc_def && page_model.assoc_def.assoc_type_uri) {
                case undefined:
                    // Note: for non-composite topics the field's assoc_def is undefined.
                    // We treat this like a composition here.
                case "dm4.core.composition_def":
                    return render_input()
                case "dm4.core.aggregation_def":
                    return render_combobox()
                default:
                    throw "TextRendererError: \"" + page_model.assoc_def.assoc_type_uri +
                        "\" is an unexpected assoc type URI"
                }
            } else {
                return render_textarea()
            }

            function render_input() {
                var input = dm4c.render.input(page_model)
                render(input)
                return input
            }

            function render_textarea() {
                var textarea = $("<textarea>").attr("rows", page_model.rows).text(page_model.value)
                render(textarea)
                return textarea
            }

            function render_combobox() {
                // fetch all instances                                         // fetch_composite=false, sort=true
                var topics = dm4c.restc.get_topics(page_model.object_type.uri, false, true).items  
                //
                var combobox = create_combobox()
                //
                render(combobox.dom)
                return combobox

                function create_combobox() {
                    var combobox = dm4c.ui.combobox()
                    // add items
                    for (var i in topics) {
                        combobox.add_item({label: topics[i].value, value: topics[i].id})
                    }
                    // select item
                    combobox.select_by_label(page_model.value)
                    //
                    return combobox
                }
            }

            function render(form_element) {
                parent_element.append(form_element)
            }
        }
    }
})
