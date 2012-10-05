(function() {
    dm4c.add_page_renderer("dm4.webclient.association_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(assoc) {
            topic_1 = assoc.get_topic_1()
            topic_2 = assoc.get_topic_2()
            var role_type_1 = assoc.get_role_type_1()
            var role_type_2 = assoc.get_role_type_2()
            // association type
            var assoc_type = dm4c.get_association_type(assoc.type_uri)
            dm4c.render.field_label("Association Type")
            dm4c.render.page(assoc_type.value)
            // 2 topics
            dm4c.render.field_label("Topics")
            render_assoc_role(topic_1, role_type_1)
            render_assoc_role(topic_2, role_type_2)
            // values
            var render_mode = dm4c.render.page_model.mode.INFO
            var page_model = create_page_model(assoc, render_mode)
            // dm4c.fire_event("pre_render_page", assoc, page_model)    // ### TODO
            render_page_model(page_model, render_mode)
            //
            dm4c.render.association_associations(assoc.id)
        },

        render_form: function(assoc) {
            var role_type_menu_1 = dm4c.render.topic_menu("dm4.core.role_type", assoc.role_1.role_type_uri)
            var role_type_menu_2 = dm4c.render.topic_menu("dm4.core.role_type", assoc.role_2.role_type_uri)
            // association type
            var assoc_type_menu = dm4c.render.topic_menu("dm4.core.assoc_type", assoc.type_uri) // a GUIToolkit Menu obj
            dm4c.render.field_label("Association Type")
            dm4c.render.page(assoc_type_menu.dom)
            // 2 topics
            dm4c.render.field_label("Topics")
            render_assoc_role_editor(topic_1, role_type_menu_1)
            render_assoc_role_editor(topic_2, role_type_menu_2)
            // values
            var render_mode = dm4c.render.page_model.mode.FORM
            var page_model = create_page_model(assoc, render_mode)
            // dm4c.fire_event("pre_render_form", topic, page_model)    // ### TODO
            render_page_model(page_model, render_mode)
            //
            return function() {
                // values
                var assoc_model = dm4c.render.page_model.build_object_model(page_model)
                // association type
                assoc_model.type_uri = assoc_type_menu.get_selection().value
                // 2 role types
                assoc_model.role_1 = {
                    topic_id: assoc.role_1.topic_id,
                    role_type_uri: role_type_menu_1.get_selection().value
                }
                assoc_model.role_2 = {
                    topic_id: assoc.role_2.topic_id,
                    role_type_uri: role_type_menu_2.get_selection().value
                }
                //
                dm4c.do_update_association(assoc_model)
            }
        }
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    var topic_1, topic_2

    function render_assoc_role(topic, role_type) {
        _render_assoc_role(topic, role_type.value)
    }

    function render_assoc_role_editor(topic, role_type_menu) {
        _render_assoc_role(topic, role_type_menu.dom)
    }

    function _render_assoc_role(topic, role_type_element) {
        var topic_div = $("<div>").text(topic.get_type().value + ": \"" + topic.value + "\"")
        var role_type_div = $("<div>")
            .append($("<div>").addClass("field-label").text("Role Type"))
            .append($("<div>").append(role_type_element))
        dm4c.render.page($("<div>").addClass("assoc-role")
            .append(topic_div).append(role_type_div))
    }

    // === Page Model ===

    /**
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object). ### FIXDOC
     */
    function create_page_model(assoc, render_mode) {
        return dm4c.render.page_model.create_page_model(assoc, undefined, "", assoc, render_mode)
    }

    /**
     * @param   page_model      the page model to render. If undefined nothing is rendered.
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object). ### FIXDOC
     */
    function render_page_model(page_model, render_mode) {
        dm4c.render.page_model.render_page_model(page_model, render_mode, 0, $("#page-content"))
    }
})()
