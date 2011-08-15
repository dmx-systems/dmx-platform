function SearchResultRenderer(topic, field) {

    this.render_field = function() {
        // retrieve search result
        var traversal_filter = {assoc_type_uri: "dm4.webclient.search_result_item"}
        var result_items = dm4c.restc.get_related_topics(topic.id, traversal_filter)    // ### FIXME: sorting?
        // render field label
        dm4c.render.field_label(field, " (" + result_items.length + ")")
        // render field value
        return dm4c.render.topic_list(result_items)
    }
}
