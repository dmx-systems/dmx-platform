function SearchResultRenderer(field_model) {

    this.render_field = function(parent_element) {
        // fetch search result
        var traversal_filter = {assoc_type_uri: "dm4.webclient.search_result_item"}
        var result = dm4c.restc.get_related_topics(field_model.toplevel_topic.id, traversal_filter, true,
            dm4c.MAX_RESULT_SIZE)
        // render field label
        dm4c.render.field_label(field_model, parent_element, result)
        // render field value
        parent_element.append(dm4c.render.topic_list(result.items))
    }
}
