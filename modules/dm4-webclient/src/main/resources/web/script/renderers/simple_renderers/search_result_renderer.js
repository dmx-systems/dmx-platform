dm4c.add_simple_renderer("dm4.webclient.search_result_renderer", {

    render_info: function(page_model, parent_element) {
        // fetch search result
        var traversal_filter = {assoc_type_uri: "dm4.webclient.search_result_item"}
        var result = dm4c.restc.get_topic_related_topics(page_model.toplevel_object.id, traversal_filter, true,
            dm4c.MAX_RESULT_SIZE)
        // render
        dm4c.render.field_label(page_model, parent_element, result)
        parent_element.append(dm4c.render.topic_list(result.items))
    }
})
