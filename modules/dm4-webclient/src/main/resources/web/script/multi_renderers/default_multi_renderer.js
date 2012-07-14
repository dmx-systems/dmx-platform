dm4c.add_multi_renderer("dm4.webclient.default_multi_renderer", {

    render_fields: function(page_models, level, parent_element) {
        var topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
        for (var i = 0; i < page_models.length; i++) {
            topic_renderer.render_page_model(page_models[i], "page", level, parent_element)
        }
    },

    render_form_elements: function(page_models, level, parent_element) {
        var topic_renderer = dm4c.get_page_renderer("dm4.webclient.topic_renderer")
        for (var i = 0; i < page_models.length; i++) {
            topic_renderer.render_page_model(page_models[i], "form", level, parent_element)
        }
        render_add_button(page_models, level, parent_element)
        //
        return function() {
            var values = []
            //
            for (var i = 0; i < page_models.length; i++) {
                if (page_models[i].topic.delete) {
                    switch (page_models[i].assoc_def.assoc_type_uri) {
                    case "dm4.core.composition_def":
                        values.push(dm4c.DEL_PREFIX + page_models[i].topic.id)
                        break
                    case "dm4.core.aggregation_def":
                        // do nothing
                        break
                    default:
                        throw "TopicRendererError: \"" + page_models[i].assoc_def.assoc_type_uri +
                            "\" is an unexpected assoc type URI"
                    }
                } else {
                    var value = topic_renderer.build_topic_model(page_models[i])
                    if (value != null) {
                        values.push(value)
                    }
                }
            }
            return values
        }

        function render_add_button(page_models, level, parent_element) {
            var topic_type = page_models[0].topic_type
            var add_button = dm4c.ui.button(do_add, "Add " + topic_type.value)
            var add_button_div = $("<div>").addClass("add-button").append(add_button)
            parent_element.append(add_button_div)

            function do_add() {
                // extend page model
                var topic = dm4c.empty_topic(topic_type.uri)
                var assoc_def      = page_models[0].assoc_def
                var field_uri      = page_models[0].uri
                var toplevel_topic = page_models[0].toplevel_topic
                var page_model = topic_renderer.create_page_model(topic, assoc_def, field_uri, toplevel_topic,
                    "editable")
                page_models.push(page_model)
                // render page model
                topic_renderer.render_page_model(page_model, "form", level, add_button_div, true)  // incremental=true
            }
        }
    }
})
