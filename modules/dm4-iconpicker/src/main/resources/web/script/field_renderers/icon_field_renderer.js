function IconFieldRenderer(field_model) {

    this.render_field = function(parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(render_icon(field_model.value))
    }

    this.render_form_element = function(parent_element) {
        var picked_icon = null  // a topic of type "dm4.webclient.icon"
        var image = render_icon(field_model.value)
        parent_element.append(image.after(dm4c.ui.button(do_open_iconpicker, "Choose")))
        //
        return function() {
            // prevent the field from being updated if no icon has been selected
            if (picked_icon == null) {
                return null
            }
            //
            if (field_model.uri) {
                // An instance of an Icon's parent type is edited.
                // Note: aggregation is assumed ### FIXME: support composition as well
                return dm4c.REF_PREFIX + picked_icon.id
            } else {
                // An Icon instance itself is edited.
                return picked_icon.value
            }
        }

        function do_open_iconpicker() {
            // query icon topics
            var icon_topics = dm4c.restc.get_topics("dm4.webclient.icon", false, true).items    // fetch_composite=false
                                                                                                // sort=true
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

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_icon(icon_src) {
        return $("<img>")
            .attr({src: icon_src, title: icon_src})
            .addClass("type-icon")
    }
}
