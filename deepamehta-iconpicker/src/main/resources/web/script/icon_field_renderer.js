function IconFieldRenderer(topic, field) {

    var picked_icon

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
        return picked_icon.value
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
