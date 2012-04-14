function HTMLFieldRenderer(field_model) {

    this.render_field = function(parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value)
    }

    this.render_form_element = function(parent_element) {
        parent_element.append($("<textarea>")
            .attr({id: "field_" + field_model.uri, rows: field_model.rows})
            .text(field_model.value)
        )
        CKEDITOR.replace("field_" + field_model.uri, {
            customConfig: "/script/config/ckeditor_config.js"
        })
    }

    this.read_form_value = function() {
        return CKEDITOR.instances["field_" + field_model.uri].getData()
    }
}
