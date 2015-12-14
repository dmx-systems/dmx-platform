dm4c.add_simple_renderer("dm4.webclient.default_composite_renderer",

    new function() {

        this.render_info = function(page_model, parent_element, level) {
            render_page_model(page_model, parent_element, dm4c.render.page_model.mode.INFO, level)
        }

        this.render_form = function(page_model, parent_element, level) {
            render_page_model(page_model, parent_element, dm4c.render.page_model.mode.FORM, level)
            // Note: no form processing function is returned here. At the moment it is hardcoded.
            // See dm4c.render.page_model.build_object_model().
            // ### TODO: make composite form processing pluggable as well?
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
                    dm4c.render.page_model.render_page_model(child_model, render_mode, level + 1, parent_element)
                }
            }
        }
    }
)
