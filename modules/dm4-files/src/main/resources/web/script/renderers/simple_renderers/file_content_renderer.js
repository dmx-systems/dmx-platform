dm4c.add_simple_renderer("dm4.files.file_content_renderer", {

    render_info: function(page_model, parent_element) {
        render_content()

        // ------------------------------------------------------------------------------------------- Private Functions

        function render_content() {
            try {
                var path       = page_model.toplevel_object.get("dm4.files.path")
                var media_type = page_model.toplevel_object.get("dm4.files.media_type")
                var src = filerepo_URI()
                // Note: for unknown file types media_type is null
                /*if (!media_type) {
                    throw "the file's media type can't be detected";
                }*/
                if (media_type) {
                    // TODO: let plugins render the file content
                    if (media_type == "text/plain") {
                        render($("<pre>").text(dm4c.restc.get_file(path)))
                        return
                    } else if (js.begins_with(media_type, "image/")) {
                        render($("<img>").attr("src", src))
                        return
                    } else if (media_type == "application/pdf") {
                        render($("<embed>").attr({src: src, type: media_type,
                            width: "100%", height: dm4c.page_panel.height}))
                        return
                        // return $("<iframe>").attr({src: src, width: "100%",
                        //     height: dm4c.topicmap_renderer.canvas_height, frameborder: 0})
                    } else if (js.begins_with(media_type, "audio/")) {
                        render($("<embed>").attr({src: src, width: "95%", height: 64, bgcolor: "#ffffff"})
                            .css("margin-top", "2em"))
                        return
                        // var content = "<audio controls=\"\" src=\"" + src + "\"></audio>"
                    } else if (js.begins_with(media_type, "video/")) {
                        // Note: default embed element is used
                        // var content = "<video controls=\"\" src=\"" + src + "\"></video>"
                    } else {
                        // Note: default embed element is used
                        // throw "media type \"" + media_type + "\" is not supported"
                    }
                }
                render($("<embed>").attr({src: src, type: media_type, width: "100%",
                    height: 0.75 * dm4c.page_panel.width, bgcolor: "#ffffff"}))
                // Note: "bgcolor" is a quicktime plugin attribute.
                // We want a white background also in Chrome (in Chrome default background is black).
            } catch (e) {
                parent_element.addClass("ui-state-error")
                render("FileContentRendererError: " + e)
            }

            function filerepo_URI() {
                // ### FIXME: principle copy in Files plugin's dm4c.restc.get_file()
                return "/filerepo/" + encodeURI(path)
            }

            function render(content_element) {
                parent_element.append(content_element)
            }
        }
    }
})
