function HTMLFieldRenderer(topic, field, rel_topics) {

    this.render_field = function() {
        dm4c.render.field_label(field)
        return field.value
    }

    this.render_form_element = function() {
        return $("<textarea>")
            .attr({id: "field_" + field.uri, rows: field.rows})
            .text(field.value)
    }

    this.post_render_form_element = function() {
        CKEDITOR.replace("field_" + field.uri, {
            customConfig: "/script/config/ckeditor_config.js"
        })
    }

    this.read_form_value = function() {
        return CKEDITOR.instances["field_" + field.uri].getData()
    }
}
