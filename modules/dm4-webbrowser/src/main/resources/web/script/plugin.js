dm4c.add_plugin("de.deepamehta.webbrowser", function() {

    // === Webclient Listeners ===

    dm4c.add_listener("topic_commands", function(topic) {

        if (topic.type_uri == "dm4.webbrowser.url") {
            return [
                {
                    is_separator: true,
                    context: "context-menu"
                },
                {
                    label:   "Open URL",
                    handler: do_open_url,
                    context: ["context-menu", "detail-panel-show"]
                },
                {
                    label:   "Open URL in new window",
                    handler: do_open_url_external,
                    context: ["context-menu", "detail-panel-show"]
                }
            ]
        }

        // === Event Handler ===

        function do_open_url() {
            var webpage = get_webpage(topic)
            if (!webpage) {
                // Note: a Webpage *aggregates* an URL (see migration2.json),
                // so we must use the REF_PREFIX notation here
                webpage = dm4c.create_topic("dm4.webbrowser.webpage", {
                    "dm4.webbrowser.url": dm4c.REF_PREFIX + topic.id
                })
            }
            dm4c.do_reveal_related_topic(webpage.id, "show")
        }

        function do_open_url_external() {
            window.open(js.absolute_http_url(topic.value), "_blank")
        }
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * Fetches and returns the Webpage topic for the given URL.
     * If there is no such Webpage topic undefined is returned.
     */
    function get_webpage(url_topic) {
        var webpages = dm4c.restc.get_topic_related_topics(url_topic.id, {
            assoc_type_uri: "dm4.core.aggregation",
            my_role_type_uri: "dm4.core.child",
            others_role_type_uri: "dm4.core.parent",
            others_topic_type_uri: "dm4.webbrowser.webpage"
        }).items
        //
        if (webpages.length > 1) {
            alert("WARNING: Data inconsistency\n\nThere are " + webpages.length + " webpages for URL \"" +
                url_topic.value + "\" (expected is one)")
        }
        //
        return webpages[0]
    }
})
