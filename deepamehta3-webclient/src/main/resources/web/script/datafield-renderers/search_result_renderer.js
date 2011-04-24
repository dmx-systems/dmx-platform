function SearchResultRenderer(topic, field) {

    this.render_field = function() {
        // alert("SearchResultRenderer\n\ntopic=" + JSON.stringify(topic) + "\n\nfield=" + JSON.stringify(field))
        var topics = dm3c.restc.get_related_topics(topic.id, "dm3.webclient.search_result_item")
        // field label
        dm3c.render.field_label(field, " (" + topics.length + ")")
        // field value
        return dm3c.render_topic_list(topics)
    }
}
