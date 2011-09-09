function FileContentRenderer(topic, field) {

    this.render_field = function(field_value_div) {
        return render_content(field_value_div)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_content(field_value_div) {
        try {
            var path       = topic.composite["dm4.files.path"]
            var size       = topic.composite["dm4.files.size"]
            var media_type = topic.composite["dm4.files.media_type"]
            var src = local_resource_URI()
            // Note: for unknown file types media_type is null
            /*if (!media_type) {
                throw "the file's media type can't be detected";
            }*/
            if (media_type) {
                // TODO: let plugins render the file content
                if (media_type == "text/plain") {
                    return $("<pre>").text(dm4c.restc.get_resource("file:" + path, media_type, size))
                } else if (js.begins_with(media_type, "image/")) {
                    return $("<img>").attr("src", src)
                } else if (media_type == "application/pdf") {
                    return $("<embed>").attr({src: src, type: media_type,
                        width: "100%", height: dm4c.canvas.canvas_height})
                    // return $("<iframe>").attr({src: src, width: "100%",
                    //     height: dm4c.canvas.canvas_height, frameborder: 0})
                } else if (js.begins_with(media_type, "audio/")) {
                    // Note: 16px is the height of the Quicktime control. ### FIXME for other players
                    return $("<embed>").attr({src: src, width: "95%", height: 16, bgcolor: "#ffffff"})
                        .css({"margin-top": "3em", "margin-bottom": "1em"})
                    // var content = "<audio controls=\"\" src=\"" + src + "\"></audio>"
                } else if (js.begins_with(media_type, "video/")) {
                    return $("<embed>").attr({src: src, type: media_type, width: "100%",
                        height: 0.75 * detail_panel_width, bgcolor: "#ffffff"})
                    // var content = "<video controls=\"\" src=\"" + src + "\"></video>"
                }
            }
            return $("<embed>").attr({src: src, width: "100%", height: dm4c.canvas.canvas_height})
            // throw "media type \"" + media_type + "\" is not supported"
        } catch (e) {
            field_value_div.addClass("ui-state-error")
            return "FileContentRendererError: " + e
        }

        function local_resource_URI() {
            var uri = "/proxy/file:" + encodeURIComponent(path) + "?size=" + size
            if (media_type) {
                uri += "&type=" + encodeURIComponent(media_type)  // media_type might contain + char ("image/svg+xml")
            }
            return uri
        }
    }
}
