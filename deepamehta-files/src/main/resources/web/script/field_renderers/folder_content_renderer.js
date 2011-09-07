function FolderContentRenderer(topic, field) {

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return render_content()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_content() {
        var path = topic.composite["dm4.files.path"]
        var items = dm4c.restc.get_resource("file:" + path).items
        var topics = []
        for (var i = 0, item; item = items[i]; i++) {
            // error check
            if (item.kind != "file" && item.kind != "directory") {
                throw "FileContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
            }
            //
            var type_uri = item.kind == "file" ? "dm4.files.file" : "dm4.files.folder"
            topics.push({type_uri: type_uri, value: item.name, path: item.path})
        }
        // Note: topic_list() takes arbitrary objects, provided
        // they contain "type_uri" and "value" properties.
        return dm4c.render.topic_list(topics, reveal_handler)
    }

    function reveal_handler(topic) {
        return function() {
            alert(JSON.stringify(topic))
            // dm4c.get_plugin("files_plugin").create_file_topic({path: topic.path})
            return false
        }
    }
}
