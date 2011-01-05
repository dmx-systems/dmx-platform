function ReferenceFieldRenderer(doc, field, rel_topics) {

    this.render_field = function() {
        // field label
        dm3c.render.field_label(field)
        // field value
        return dm3c.render_topic_list(rel_topics)
    }

    this.render_form_element = function() {
        switch (field.editor) {
        case "checkboxes":
            var topics = dm3c.restc.get_topics(field.ref_topic_type_uri)
            var relation_div = $("<div>")
            for (var i = 0, topic; topic = topics[i]; i++) {
                var attr = {type: "checkbox", id: topic.id, name: "relation_" + field.uri}
                if (js.includes(rel_topics, function(t) {
                        return t.id == topic.id
                    })) {
                    attr.checked = "checked"
                }
                relation_div.append($("<label>").append($("<input>").attr(attr)).append(topic.label))
            }
            return relation_div
        }
    }

    this.read_form_value = function() {
        // TODO: updating reference fields should run at server-side (in a transaction)
        switch (field.editor) {
        case "checkboxes":
            $("input:checkbox[name=relation_" + field.uri + "]").each(
                function() {
                    var checkbox = this
                    var was_checked_before = js.includes(dm3c.get_doctype_impl(doc).topic_buffer[field.uri],
                        function(topic) {
                            return topic.id == checkbox.id
                        }
                    )
                    if (checkbox.checked) {
                        if (!was_checked_before) {
                            dm3c.create_relation("RELATION", doc.id, checkbox.id)
                        }
                    } else {
                        if (was_checked_before) {
                            dm3c.delete_relation(dm3c.restc.get_relation(doc.id, checkbox.id).id)
                        }
                    }
                }
            )
            // prevent this field from being updated
            return null
        }
    }
}
