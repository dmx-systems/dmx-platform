// This is dead DM4 code. TODO: adapt to DM5
(function() {

    var TRAILBLAZER_FEATURE = false                 // trailblazer is switched off
    var PROXY_URL = "http://localhost:8080/proxy/"  // used by trailblazer ### FIXME: /proxy is obsolete

    dm4c.add_page_renderer("dmx.webbrowser.webpage_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {
            var url = js.absolute_http_url(topic.get("dmx.base.url"))
            var iframe = $("<iframe>").attr({width: "100%", height: "100%", frameborder: 0})
            //
            if (TRAILBLAZER_FEATURE) {
                activate_trailblazer()
            }
            //
            dm4c.render.page(iframe.attr("src", url))

            function activate_trailblazer() {

                url = PROXY_URL + url
                iframe.attr("name", "dmx.webbrowser.webpage").load(prepare_page)

                function prepare_page() {
                    var doc = window.frames["dmx.webbrowser.webpage"].document
                    $("a", doc).click(function(event) {
                        // var str = "<" + this.tagName + "> element clicked\nhref=\"" + this.href + "\""
                        // alert(str)
                        follow_link(this.href)
                        return false
                    })
                }

                function follow_link(url) {
                    var webpage = dm4c.create_topic("dmx.webbrowser.webpage", {"dmx.base.url": url})
                    dm4c.create_association("dmx.core.association",
                        {topic_id: topic.id, role_type_uri: "dmx.core.default"},
                        {topic_id: webpage.id, role_type_uri: "dmx.core.default"}
                    )
                    dm4c.do_reveal_related_topic(webpage.id, "show")
                }
            }
        },

        page_css: {overflow: "visible"}
    })
})()
