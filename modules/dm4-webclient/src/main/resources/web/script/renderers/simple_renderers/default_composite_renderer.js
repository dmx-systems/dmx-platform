dm4c.add_simple_renderer("dm4.webclient.default_composite_renderer", new function() {

    this.render_info = function(page_model, parent_element, level) {
        dm4c.render.field_label(page_model, parent_element)
        render_page_model(page_model, parent_element, dm4c.render.page_model.mode.INFO, level)
    }

    this.render_form = function(page_model, parent_element, level) {
        dm4c.render.field_label(page_model, parent_element)
        render_page_model(page_model, parent_element, dm4c.render.page_model.mode.FORM, level)
        //
        return function() {
            var object_model = {
                id:       page_model.object.id,
                type_uri: page_model.object.type_uri,
                // Note: the type URI is not strictly required for server-side processing, but for the
                // client-side "pre_update_topic"/"pre_update_association" listeners as they usually
                // examine the topic's/association's type.
                childs: {}
            }
            for (var child_type_uri in page_model.childs) {
                var child_model = page_model.childs[child_type_uri]
                if (child_model.type == dm4c.render.page_model.type.MULTI) {
                    // cardinality "many"
                    var values = child_model.read_form_values()
                    object_model.childs[child_type_uri] = values
                } else {
                    // cardinality "one"
                    var value = dm4c.render.page_model.build_object_model(child_model)
                    if (value != null) {
                        object_model.childs[child_type_uri] = value
                    }
                }
            }
            return object_model
        }
    }

    function render_page_model(page_model, parent_element, render_mode, level) {
        for (var child_type_uri in page_model.childs) {
            var child_model = page_model.childs[child_type_uri]
            if (child_model.type == dm4c.render.page_model.type.MULTI) {
                // cardinality "many"
                var multi_box = dm4c.render.page_model.render_box(child_model, parent_element, render_mode, level)
                child_model[render_mode.render_func_name_multi](multi_box, level + 1)
            } else {
                // cardinality "one"
                dm4c.render.page_model.render_page_model(child_model, parent_element, render_mode, level + 1)
            }
        }
    }
})
