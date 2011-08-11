function webbrowser_plugin() {



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.add_topic_commands = function(topic) {

        if (topic.type_uri == "dm4.webbrowser.url") {
            return [{label: "Open URL", handler: do_open_url, context: "detail-panel-show"}]
        }

        function do_open_url() {
            window.open(js.absolute_http_url(topic.value), "_blank")
        }
    }
}
