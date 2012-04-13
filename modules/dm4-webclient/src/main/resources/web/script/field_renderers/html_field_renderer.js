function HTMLFieldRenderer(field_model) {

    this.render_field = function() {
        dm4c.render.field_label(field_model)
        return field_model.value
    }

    this.render_form_element = function() {
        return $("<textarea>")
            .attr({id: "field_" + field_model.uri, rows: field_model.rows})
            .text(field_model.value)
    }

    this.post_render_form_element = function() {
        CKEDITOR.replace("field_" + field_model.uri, {
            customConfig: "/script/config/ckeditor_config.js"
        })
    }

    this.read_form_value = function() {
        return CKEDITOR.instances["field_" + field_model.uri].getData()
    }
}
