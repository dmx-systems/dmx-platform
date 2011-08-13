function WebpageRenderer() {

    var PROXY_URL = "http://localhost:8080/proxy/"


    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {
        var url = PROXY_URL + js.absolute_http_url(topic.composite["dm4.webbrowser.url"])
        var iframe = $("<iframe>").load(prepare_page)
            .attr({name: "dm4.webbrowser.webpage", src: url, width: "100%", height: "100%", frameborder: 0})
        dm4c.render.page(iframe)

        function prepare_page() {
            var doc = window.frames["dm4.webbrowser.webpage"].document
            $("a", doc).click(function(event) {
                // var str = "<" + this.tagName + "> element clicked\nhref=\"" + this.href + "\""
                // alert(str)
                follow_link(this.href)
                return false
            })
        }

        function follow_link(url) {
            var webpage = dm4c.create_topic("dm4.webbrowser.webpage", {"dm4.webbrowser.url": url})
            dm4c.create_association("dm4.core.association",
                {topic_id: topic.id, role_type_uri: "dm4.core.default"},
                {topic_id: webpage.id, role_type_uri: "dm4.core.default"}
            )
            dm4c.do_reveal_related_topic(webpage.id)
        }
    }

    this.page_css = function() {
        return {overflow: "visible"}
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

}
