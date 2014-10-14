dm4c.add_page_renderer("dm4.typeeditor.roletype_renderer", {
    
    // === Page Renderer Implementation ===

    render_page: function(topic) {
        dm4c.render.field_label("Name")
        dm4c.render.page(topic.value)
        //
        dm4c.render.field_label("URI")
        dm4c.render.page(topic.uri)
        //
        dm4c.render.topic_associations(topic.id)
    },

    render_form: function(topic) {
        var name_input = dm4c.render.input(topic.value)      // a jQuery <input> element
        dm4c.render.field_label("Name")
        dm4c.render.page(name_input)
        //
        var uri_input = dm4c.render.input(topic.uri)         // a jQuery <input> element
        dm4c.render.field_label("URI")
        dm4c.render.page(uri_input)
        //
        return function() {
            var topic_model = {
                id: topic.id,
                uri: $.trim(uri_input.val()),
                value: $.trim(name_input.val())
            }
            dm4c.do_update_topic(topic_model)
        }
    }
})
