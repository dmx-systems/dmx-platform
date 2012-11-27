(function() {
    function AssociationTypeRenderer() {

        // === Page Renderer Implementation ===

        this.render_page = function(assoc) {
            var assoc_type = dm4c.get_association_type(assoc.uri)
            this.render_type_page(assoc_type)
        }

        this.render_form = function(assoc) {
            var assoc_type = dm4c.get_association_type(assoc.uri)
            var type_model_func = this.render_type_form(assoc_type)
            return function() {
                dm4c.do_update_association_type(type_model_func())
            }
        }
    }

    AssociationTypeRenderer.prototype = new TypeRenderer()

    dm4c.add_page_renderer("dm4.typeeditor.assoctype_renderer", new AssociationTypeRenderer())
})()
