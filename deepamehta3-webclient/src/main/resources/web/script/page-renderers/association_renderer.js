function AssociationRenderer() {

    var assoc_type_menu   // a UIHelper Menu object
    var role_type_menu_1, role_type_menu_2

    var topic_1, topic_2
    var role_type_1, role_type_2

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(assoc) {
        var assoc_type = dm3c.type_cache.get_association_type(assoc.type_uri)
        dm3c.render.field_label("Association Type")
        dm3c.render.field_value(assoc_type.value)
        //
        topic_1 = assoc.get_topic_1()
        role_type_1 = assoc.get_role_type_1()
        dm3c.render.field_label("Topic 1")
        dm3c.render.field_value("\"" + topic_1.value + "\" (" + topic_1.get_type().value + ")")
        dm3c.render.field_label("Role Type 1")
        dm3c.render.field_value(role_type_1.value)
        //
        topic_2 = assoc.get_topic_2()
        role_type_2 = assoc.get_role_type_2()
        dm3c.render.field_label("Topic 2")
        dm3c.render.field_value("\"" + topic_2.value + "\" (" + topic_2.get_type().value + ")")
        dm3c.render.field_label("Role Type 2")
        dm3c.render.field_value(role_type_2.value)
    }

    this.render_form = function(assoc) {
        assoc_type_menu = dm3c.render.topic_menu("dm3.core.assoc_type", assoc.type_uri)
        dm3c.render.field_label("Association Type")
        dm3c.render.field_value(assoc_type_menu.dom)
        //
        role_type_menu_1 = dm3c.render.topic_menu("dm3.core.role_type", assoc.role_1.role_type_uri)
        dm3c.render.field_label("Topic 1")
        dm3c.render.field_value("\"" + topic_1.value + "\" (" + topic_1.get_type().value + ")")
        dm3c.render.field_label("Role Type 1")
        dm3c.render.field_value(role_type_menu_1.dom)
        //
        role_type_menu_2 = dm3c.render.topic_menu("dm3.core.role_type", assoc.role_2.role_type_uri)
        dm3c.render.field_label("Topic 2")
        dm3c.render.field_value("\"" + topic_2.value + "\" (" + topic_2.get_type().value + ")")
        dm3c.render.field_label("Role Type 2")
        dm3c.render.field_value(role_type_menu_2.dom)
    }

    this.process_form = function(assoc) {
        // 1) update DB and memory
        var assoc_model = build_association_model()
        // alert("association model to update: " + JSON.stringify(assoc_model))
        assoc = dm3c.update_association(assoc, assoc_model)
        dm3c.trigger_plugin_hook("post_submit_form", assoc)
        // 2) update GUI
        // ### moved to process_directives

        /**
         * Reads out values from GUI elements and builds an association model object from it.
         *
         * @return  an association model object
         */
        function build_association_model() {
            return {
                id: assoc.id,
                type_uri: assoc_type_menu.get_selection().value,
                role_1: {
                    topic_id: assoc.role_1.topic_id,
                    role_type_uri: role_type_menu_1.get_selection().value
                },
                role_2: {
                    topic_id: assoc.role_2.topic_id,
                    role_type_uri: role_type_menu_2.get_selection().value
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions
}
