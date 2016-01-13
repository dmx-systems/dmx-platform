dm4c.add_multi_renderer("dm4.webclient.default_multi_renderer", new function() {

    this.render_info = function(page_models, parent_element, level) {
        render_page_models(page_models, parent_element, dm4c.render.page_model.mode.INFO, level)
    }

    this.render_form = function(page_models, parent_element, level) {
        var single_values_div = $("<div>")      // new instances are added to this element ("Add" button)
        parent_element.append(single_values_div)
        render_page_models(page_models, single_values_div, dm4c.render.page_model.mode.FORM, level)
        render_add_button()
        //
        return function() {
            var values = []
            //
            for (var i = 0; i < page_models.length; i++) {
                if (page_models[i].object.delete) {
                    values.push(dm4c.DEL_ID_PREFIX + page_models[i].object.id)
                } else {
                    var value = dm4c.render.page_model.build_object_model(page_models[i])
                    if (value != null) {
                        values.push(value)
                    }
                }
            }
            return values
        }

        function render_add_button() {
            var topic_type = page_models[0].object_type
            var add_button = dm4c.ui.button({on_click: do_add, label: "Add " + topic_type.value})
            var add_button_div = $("<div>").addClass("add-button").append(add_button)
            parent_element.append(add_button_div)

            function do_add() {
                // extend page model
                var topic = dm4c.empty_topic(topic_type.uri)
                var assoc_def         = page_models[0].assoc_def
                var field_uri         = page_models[0].uri
                var parent_page_model = page_models[0].parent
                var page_model = dm4c.render.page_model.create_page_model(topic, assoc_def, field_uri,
                    dm4c.render.page_model.mode.FORM, parent_page_model)
                page_models.push(page_model)
                // render page model
                dm4c.render.page_model.render_page_model(page_model, single_values_div,
                    dm4c.render.page_model.mode.FORM, level)
            }
        }
    }

    function render_page_models(page_models, parent_element, render_mode, level) {
        for (var i = 0; i < page_models.length; i++) {
            dm4c.render.page_model.render_page_model(page_models[i], parent_element, render_mode, level)
        }
    }
})
