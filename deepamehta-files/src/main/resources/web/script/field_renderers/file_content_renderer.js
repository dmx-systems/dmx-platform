function FileContentRenderer(topic, field) {


    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return render_content()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_content() {
        try {
            var path       = topic.composite["dm4.files.path"]
            var size       = topic.composite["dm4.files.size"]
            var media_type = topic.composite["dm4.files.media_type"]
            // Note: for unknown file types media_type is null
            if (!media_type) {
                throw "the file's media type can't be detected";
            }
            // TODO: let plugins render the file content
            if (media_type == "text/plain") {
                var text = dm4c.restc.get_resource("file:" + path, media_type, size)
                return "<pre>" + text + "</pre>"
            } else if (js.begins_with(media_type, "image/")) {
                return "<img src=\"" + local_resource_URI() + "\"></img>"
            } else if (media_type == "application/pdf") {
                return "<embed src=\"" + local_resource_URI() +
                    "\" width=\"100%\" height=\"100%\"></embed>"
            } else if (js.begins_with(media_type, "audio/")) {
                return "<embed src=\"" + local_resource_URI() +
                    "\" width=\"95%\" height=\"80\"></embed>"
                // var content = "<audio controls=\"\" src=\"" + local_resource_URI() + "\"></audio>"
            } else if (js.begins_with(media_type, "video/")) {
                return "<embed src=\"" + local_resource_URI() + "\"></embed>"
                // var content = "<video controls=\"\" src=\"" + local_resource_URI() + "\"></video>"
            }
            throw "media type \"" + media_type + "\" is not supported"
        } catch (e) {
            return $("<div>").addClass("ui-state-error").text("FileContentRendererError: " + e)
        }

        function local_resource_URI() {
            return "/proxy/file:" + path + "?type=" + media_type + "&size=" + size;
        }
    }
}
