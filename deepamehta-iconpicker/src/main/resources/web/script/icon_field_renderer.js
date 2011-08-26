function IconFieldRenderer(topic, field, rel_topics) {

    var plugin = dm4c.get_plugin("iconpicker_plugin")

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return render_icon(topic)
    }

    this.render_form_element = function() {
        var image = render_icon(topic)
        return image.after(dm4c.ui.button(do_open_iconpicker, "Choose"))

        function do_open_iconpicker() {
            // query icon topics
            var icon_topics = dm4c.restc.get_topics("dm4.webclient.icon_src")
            // fill dialog with icons
            $("#iconpicker-dialog").empty()
            for (var i = 0, icon_topic; icon_topic = icon_topics[i]; i++) {
                $("#iconpicker-dialog").append(render_icon(icon_topic).click(do_pick_icon(icon_topic)))
            }
            // open dialog
            $("#iconpicker-dialog").dialog("open")

            function do_pick_icon(icon_topic) {
                return function() {
                    $("#iconpicker-dialog").dialog("close")
                    image.attr({src: icon_topic.value, title: icon_topic.value})
                }
            }
        }
    }

    this.read_form_value = function() {
        var icons = dm4c.get_doctype_impl(topic).topic_buffer[field.uri]
        var old_icon_id = icons.length && icons[0].id
        var new_icon_id = $("[field-uri=" + field.uri + "] img").attr("icon-topic-id")
        if (old_icon_id) {
            if (old_icon_id != new_icon_id) {
                // re-assign icon
                dm4c.delete_association(dm4c.restc.get_relation(topic.id, old_icon_id).id)
                dm4c.create_relation("RELATION", topic.id, new_icon_id)
            }
        } else if (new_icon_id) {
            // assign icon
            dm4c.create_relation("RELATION", topic.id, new_icon_id)
        }
        // prevent this field from being updated
        return null
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * @param   icon_topic    a topic of type "Icon Source"
     */
    function render_icon(icon_topic) {
        var icon_src = icon_topic.value
        return $("<img>")
            .attr({"icon-topic-id": icon_topic.id, src: icon_src, title: icon_src})
            .addClass("type-icon")
    }
}
