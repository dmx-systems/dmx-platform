function AssociationType() {

    this.get_page_renderer_class = function() {
        return /* dm3c.get_view_config(this, "js_page_renderer_class") || */ "AssociationRenderer"
    }
}
