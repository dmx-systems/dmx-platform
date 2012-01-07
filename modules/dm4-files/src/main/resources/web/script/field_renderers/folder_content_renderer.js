function FolderContentRenderer(topic, field) {

    this.render_field = function(field_value_div) {
        // field label
        dm4c.render.field_label(field)
        // field value
        return render_content(field_value_div)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_content(field_value_div) {
        try {
            var path = topic.get("dm4.files.path")
            var items = dm4c.restc.get_resource("file:" + path).items
            var topics = []
            for (var i = 0, item; item = items[i]; i++) {
                // error check
                if (item.kind != "file" && item.kind != "directory") {
                    throw "FileContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
                }
                //
                var type_uri = item.kind == "file" ? "dm4.files.file" : "dm4.files.folder"
                topics.push({type_uri: type_uri, value: item.name, kind: item.kind, path: item.path})
            }
            // Note: topic_list() takes arbitrary objects, provided
            // they contain "type_uri" and "value" properties.
            return dm4c.render.topic_list(topics, click_handler)
        } catch (e) {
            field_value_div.addClass("ui-state-error")
            return "FolderContentRendererError: " + e
        }
    }

    function click_handler(item) {
        if (item.kind == "file") {
            var child_topic = dm4c.restc.create_child_file_topic(topic.id, item.path)
        } else if (item.kind == "directory") {
            var child_topic = dm4c.restc.create_child_folder_topic(topic.id, item.path)
        } else {
            throw "FileContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
        }
        //
        dm4c.do_reveal_related_topic(child_topic.id)
    }
}
