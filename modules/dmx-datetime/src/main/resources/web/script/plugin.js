dm4c.add_plugin("systems.dmx.datetime", function() {

    var month_names = [undefined, "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

    // === Webclient Listeners ===

    dm4c.add_listener("option_topics", function(page_model) {
        var topic_type_uri = page_model.object_type.uri
        if (topic_type_uri == "dm4.datetime.month") {
            var month_topics = dm4c.restc.get_topics(topic_type_uri, false, true)   // include_childs=false, sort=true
            month_topics.forEach(function(month_topic) {
                month_topic.value = month_names[month_topic.value]
            })
            return month_topics
        }
    })
})
