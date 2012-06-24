function HTMLFieldRenderer(field_model) {
    this.field_model = field_model
}

HTMLFieldRenderer.prototype.render_field = function(parent_element) {
    dm4c.render.field_label(this.field_model, parent_element)
    parent_element.append(this.field_model.value)
}

HTMLFieldRenderer.prototype.render_form_element = function(parent_element) {
    var field_model = this.field_model      // needed in returned function
    parent_element.append($("<textarea>")
        .attr({id: "field_" + field_model.uri, rows: field_model.rows})
        .text(field_model.value)
    )
    CKEDITOR.replace("field_" + field_model.uri, {
        customConfig: "/de.deepamehta.webclient/script/config/ckeditor_config.js"
    })
    //
    return function() {
        return CKEDITOR.instances["field_" + field_model.uri].getData()
    }
}
