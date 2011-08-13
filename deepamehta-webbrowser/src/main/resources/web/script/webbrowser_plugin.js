function webbrowser_plugin() {

    dm4c.register_page_renderer("/de.deepamehta.webbrowser/script/webpage_renderer.js")



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.topic_commands = function(topic) {

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

        function do_open_url() {
            var webpage = dm4c.create_topic("dm4.webbrowser.webpage", {"dm4.webbrowser.url$id": topic.id})
            dm4c.do_reveal_related_topic(webpage.id)
        }

        function do_open_url_external() {
            window.open(js.absolute_http_url(topic.value), "_blank")
        }
    }
}
