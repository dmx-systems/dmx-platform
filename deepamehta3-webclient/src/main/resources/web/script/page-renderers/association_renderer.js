function AssociationRenderer() {

    var type_menu   // a UIHelper Menu object

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(assoc) {
        var assoc_type = dm3c.type_cache.get_association_type(assoc.type_uri)
        //
        dm3c.render.field_label("Association Type")
        dm3c.render.field_value(assoc_type.value)
        //
        dm3c.render.field_label("Association Type URI")
        dm3c.render.field_value(assoc_type.uri)
    }

    this.render_form = function(assoc) {
        type_menu = dm3c.render.topic_menu("dm3.core.assoc_type", assoc.type_uri)
        dm3c.render.field_label("Association Type")
        dm3c.render.field_value(type_menu.dom)
    }

    this.process_form = function(assoc) {
        // 1) update DB and memory
        var assoc_model = build_association_model()
        // alert("association model to update: " + JSON.stringify(assoc_model))
        assoc = dm3c.update_association(assoc, assoc_model)
        dm3c.trigger_plugin_hook("post_submit_form", assoc)
        // 2) update GUI
        dm3c.canvas.update_association(assoc)
        dm3c.canvas.refresh()
        dm3c.page_panel.display(assoc)

        /**
         * Reads out values from GUI elements and builds an association model object from it.
         *
         * @return  an association model object
         */
        function build_association_model() {
            return {
                id: assoc.id,
                type_uri: type_menu.get_selection().value,
                role_1: assoc.role_1,
                role_2: assoc.role_2
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions
}
