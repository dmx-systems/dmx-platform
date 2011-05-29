function AssociationRenderer() {

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(assoc) {
        dm3c.render.field_value("<h2>Hello Association!</h2>")
        dm3c.render.field_value("<code>" + JSON.stringify(assoc) + "</code>")
    }

    this.render_form = function(assoc) {
    }

    this.process_form = function(assoc) {
    }

    // ----------------------------------------------------------------------------------------------- Private Functions
}
