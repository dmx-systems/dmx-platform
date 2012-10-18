dm4c.add_multi_renderer("dm4.webclient.checkbox_renderer", {

    render_info: function(page_models, parent_element, level) {
        var list = $("<ul>")
        for (var i = 0; i < page_models.length; i++) {
            list.append($("<li>").text(page_models[i].value))
        }
        dm4c.render.field_label(page_models[0], parent_element)
        parent_element.append(list)
    },

    render_form: function(page_models, parent_element, level) {
        var topics = option_topics(page_models[0])
        var checkboxes = {}
        dm4c.render.field_label(page_models[0], parent_element)
        // render checkboxes
        for (var i = 0; i < topics.length; i++) {
            var checkbox = dm4c.render.checkbox(is_checked(topics[i]))
            parent_element.append(checkbox)
            parent_element.append(topics[i].value)
            parent_element.append("<br>")
            checkboxes[topics[i].id] = checkbox
        }
        //
        return function() {
            if (page_models[0].assoc_def.assoc_type_uri == "dm4.core.composition_def") {
                throw "CheckboxRendererError: the Checkbox Renderer can't be used with Composition Definition " +
                    "(but only with Aggregation Definition)"
            }
            //
            var values = []
            //
            for (var topic_id in checkboxes) {
                // ### TODO: returning only the changed values would be more efficient
                if (checkboxes[topic_id].get(0).checked) {
                    values.push(dm4c.REF_PREFIX + topic_id)
                } else {
                    values.push(dm4c.DEL_PREFIX + topic_id)
                }
            }
            return values
        }

        // ### TODO: there is a copy in TextRenderer
        function option_topics(page_model) {
            var topic_type_uri = page_model.object_type.uri
            var result = dm4c.fire_event("option_topics", page_model.toplevel_object, topic_type_uri,
                                                          page_model.assoc_def)
            switch (result.length) {
            case 0:
                // fetch all instances                        // fetch_composite=false, sort=true
                return dm4c.restc.get_topics(topic_type_uri, false, true).items
            case 1:
                return result[0]
            default:
                throw "CheckboxRendererError: " + result.length + " plugins are competing with " +
                    "providing the option topics for \"" + topic_type_uri + "\""
            }
        }

        function is_checked(topic) {
            return js.includes(page_models, function(page_model) {
                return page_model.object.id == topic.id
            })
        }
    }
})
