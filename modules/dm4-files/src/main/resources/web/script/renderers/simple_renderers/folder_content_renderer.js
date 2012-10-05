dm4c.add_simple_renderer("dm4.files.folder_content_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        render_content()

        // ------------------------------------------------------------------------------------------- Private Functions

        function render_content() {
            try {
                var path = page_model.toplevel_object.get("dm4.files.path")
                var items = dm4c.restc.get_directory_listing(path).items
                var topics = []
                for (var i = 0, item; item = items[i]; i++) {
                    // error check
                    if (item.kind != "file" && item.kind != "directory") {
                        throw "FolderContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
                    }
                    //
                    var type_uri = item.kind == "file" ? "dm4.files.file" : "dm4.files.folder"
                    topics.push({type_uri: type_uri, value: item.name, kind: item.kind, path: item.path})
                }
                // Note: topic_list() takes arbitrary objects, provided
                // they contain "type_uri" and "value" properties.
                parent_element.append(dm4c.render.topic_list(topics, click_handler))
            } catch (e) {
                parent_element.addClass("ui-state-error")
                parent_element.append("FolderContentRendererError: " + e)
            }
        }

        function click_handler(item) {
            if (item.kind == "file") {
                var child_topic = dm4c.restc.create_child_file_topic(page_model.toplevel_object.id, item.path)
            } else if (item.kind == "directory") {
                var child_topic = dm4c.restc.create_child_folder_topic(page_model.toplevel_object.id, item.path)
            } else {
                throw "FolderContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
            }
            //
            dm4c.do_reveal_related_topic(child_topic.id)
        }
    }
})
