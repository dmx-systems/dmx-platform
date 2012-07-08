dm4c.add_field_renderer("dm4.webclient.html_renderer", {

    render_field: function(field_model, parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value)
    },

    render_form_element: function(field_model, parent_element) {
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
})
