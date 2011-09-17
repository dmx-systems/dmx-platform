function IconFieldRenderer(topic, field) {

    var picked_icon = null

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return render_icon(field.value)
    }

    this.render_form_element = function() {
        var image = render_icon(field.value)
        return image.after(dm4c.ui.button(do_open_iconpicker, "Choose"))

        function do_open_iconpicker() {
            // query icon topics
            var icon_topics = dm4c.restc.get_topics("dm4.webclient.icon", true).items   // sort=true
            // fill dialog with icons
            $("#iconpicker-dialog").empty()
            for (var i = 0, icon_topic; icon_topic = icon_topics[i]; i++) {
                $("#iconpicker-dialog").append(render_icon(icon_topic.value).click(do_pick_icon(icon_topic)))
            }
            // open dialog
            $("#iconpicker-dialog").dialog("open")

            function do_pick_icon(icon_topic) {
                return function() {
                    // update model
                    picked_icon = icon_topic
                    // update view
                    $("#iconpicker-dialog").dialog("close")
                    image.attr({src: icon_topic.value, title: icon_topic.value})
                }
            }
        }
    }

    this.read_form_value = function() {
        if (field.uri) {
            // An instance of an Icon's parent type is edited.
            // Note: aggregation is assumed ### FIXME: support composition as well
            return picked_icon && {topic_id: picked_icon.id}
        } else {
            // An Icon instance itself is edited.
            return picked_icon && picked_icon.value
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_icon(icon_src) {
        return $("<img>")
            .attr({src: icon_src, title: icon_src})
            .addClass("type-icon")
    }
}
