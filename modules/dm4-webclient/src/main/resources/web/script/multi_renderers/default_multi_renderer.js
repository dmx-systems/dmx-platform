dm4c.add_multi_renderer("dm4.webclient.default_multi_renderer", {

    render_fields: function(field_models, level, parent_element) {
        var topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
        for (var i = 0; i < field_models.length; i++) {
            topic_renderer.render_page_model(field_models[i], "page", level, parent_element)
        }
    },

    render_form_elements: function(field_models, level, parent_element) {
        var topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
        for (var i = 0; i < field_models.length; i++) {
            topic_renderer.render_page_model(field_models[i], "form", level, parent_element)
        }
        render_add_button(field_models, level, parent_element)

        function render_add_button(page_model, level, parent_element) {
            var topic_type = page_model[0].topic_type
            var add_button = dm4c.ui.button(do_add, "Add " + topic_type.value)
            var add_button_div = $("<div>").addClass("add-button").append(add_button)
            parent_element.append(add_button_div)

            function do_add() {
                // extend page model
                var topic = dm4c.empty_topic(topic_type.uri)
                var assoc_def      = page_model[0].assoc_def
                var field_uri      = page_model[0].uri
                var toplevel_topic = page_model[0].toplevel_topic
                var _page_model = topic_renderer.create_page_model(topic, assoc_def, field_uri, toplevel_topic,
                    "editable")
                page_model.push(_page_model)
                // render page model
                topic_renderer.render_page_model(_page_model, "form", level, add_button_div, true)  // incremental=true
            }
        }
    }
})
