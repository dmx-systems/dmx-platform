function WebpageRenderer() {



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {
        var url = js.absolute_http_url(topic.composite["dm4.webbrowser.url"])
        dm4c.render.page($("<iframe>")
            .attr({src: url, width: "100%", height: "100%", frameborder: 0})
        )
    }

    this.page_css = function() {
        return {overflow: "visible"}
    }
}
