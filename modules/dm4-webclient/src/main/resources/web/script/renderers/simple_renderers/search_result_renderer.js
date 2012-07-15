dm4c.add_simple_renderer("dm4.webclient.search_result_renderer", {

    render_field: function(field_model, parent_element) {
        // fetch search result
        var traversal_filter = {assoc_type_uri: "dm4.webclient.search_result_item"}
        var result = dm4c.restc.get_related_topics(field_model.toplevel_topic.id, traversal_filter, true,
            dm4c.MAX_RESULT_SIZE)
        // render
        dm4c.render.field_label(field_model, parent_element, result)
        parent_element.append(dm4c.render.topic_list(result.items))
    }
})
