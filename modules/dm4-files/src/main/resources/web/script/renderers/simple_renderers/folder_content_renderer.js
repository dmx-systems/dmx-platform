dm4c.add_simple_renderer("dm4.files.folder_content_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        render_content()

        // ------------------------------------------------------------------------------------------- Private Functions

        function render_content() {
            try {
                var path = page_model.parent.object.get("dm4.files.path")
                parent_element.append(dm4c.render.topic_list(get_topics(path), click_handler))
            } catch (e) {
                parent_element.addClass("ui-state-error")
                parent_element.append("FolderContentRendererError: " + e)
            }
        }

        function get_topics(path) {
            var items = dm4c.restc.get_directory_listing(path).items
            var topics = []
            for (var i = 0, item; item = items[i]; i++) {
                // error check
                if (item.kind != "file" && item.kind != "directory") {
                    throw "FolderContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
                }
                // Note: dm4c.render.topic_list() takes arbitrary objects as long as they have "type_uri"
                // and "value" properties.
                var type_uri = item.kind == "file" ? "dm4.files.file" : "dm4.files.folder"
                topics.push({type_uri: type_uri, value: item.name, kind: item.kind, path: item.path})
            }
            // Note: dm4c.restc.get_directory_listing() provides no ordering guarantee
            topics.sort(function(topic_1, topic_2) {
                return topic_1.value.toLowerCase() < topic_2.value.toLowerCase() ? -1 : 1
            })
            //
            return topics
        }

        function click_handler(item, spot) {
            if (item.kind == "file") {
                var child_topic = dm4c.restc.create_child_file_topic(page_model.parent.object.id, item.path)
            } else if (item.kind == "directory") {
                var child_topic = dm4c.restc.create_child_folder_topic(page_model.parent.object.id, item.path)
            } else {
                throw "FolderContentRendererError: item has unexpected kind (\"" + item.kind + "\")"
            }
            //
            var action = spot == "label" && "show"
            dm4c.do_reveal_related_topic(child_topic.id, action)
        }
    }
})
