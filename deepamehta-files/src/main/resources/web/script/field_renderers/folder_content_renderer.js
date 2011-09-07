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
            switch (item.kind) {
            case "file":
                topics.push({type_uri: "dm4.files.file", value: item.name})
                break
            case "directory":
                topics.push({type_uri: "dm4.files.folder", value: item.name})
                break
            }
        }
        return dm4c.render.topic_list(topics, reveal_handler)

        function reveal_handler(topic) {
            return function() {
                alert(JSON.stringify(topic))
                return false
            }
        }
    }
}
