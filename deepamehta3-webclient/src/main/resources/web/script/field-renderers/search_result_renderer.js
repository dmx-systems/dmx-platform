function SearchResultRenderer(topic, field) {

    this.render_field = function() {
        // alert("SearchResultRenderer\n\ntopic=" + JSON.stringify(topic) + "\n\nfield=" + JSON.stringify(field))
        var topics = dm4c.restc.get_related_topics(topic.id, "dm4.webclient.search_result_item")
        // field label
        dm4c.render.field_label(field, " (" + topics.length + ")")
        // field value
        return dm4c.render.topic_list(topics)
    }
}
