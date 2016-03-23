dm4c.add_simple_renderer("dm4.webclient.search_result_renderer", {

    render_info: function(page_model, parent_element) {
        // fetch search result
        var traversal_filter = {assoc_type_uri: "dm4.webclient.search_result_item"}
        var topics = dm4c.restc.get_topic_related_topics(page_model.parent.object.id, traversal_filter, true)
        // render
        dm4c.render.field_label(page_model, parent_element, topics)
        parent_element.append(dm4c.render.topic_list(topics))
    }
})
