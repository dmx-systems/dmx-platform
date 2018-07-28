dm4c.add_simple_renderer("dm4.iconpicker.icon_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(dm4c.render.icon(page_model.value, page_model.value))
    },

    render_form: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var picked_icon = null                  // a topic of type "dm4.webclient.icon"
        var image = dm4c.render.icon(page_model.value, page_model.value)
        parent_element.addClass("iconpicker")
            .append(image)
            .append(dm4c.ui.button({on_click: open_choose_icon_dialog, label: "Choose"}))
        //
        return function() {
            // prevent the field from being updated if no icon has been selected
            if (picked_icon == null) {
                return null
            }
            //
            if (page_model.uri) {
                // An instance of an Icon's parent type is edited.
                // Note: aggregation is assumed ### FIXME: support composition as well
                return dm4c.REF_ID_PREFIX + picked_icon.id
            } else {
                // An Icon instance itself is edited.
                return picked_icon.value
            }
        }

        // ------------------------------------------------------------------------------------------- Private Functions

        function open_choose_icon_dialog() {
            // retrieve icon topics
            var icon_topics = dm4c.restc.get_topics("dm4.webclient.icon", false, true)  // include_childs=false,
            //                                                                          // sort=true
            var content = $()
            for (var i = 0, icon_topic; icon_topic = icon_topics[i]; i++) {
                content = content.add(dm4c.render.icon(icon_topic.value, icon_topic.value)
                    .click(do_pick_icon(icon_topic))
                )
            }
            var iconpicker_dialog = dm4c.ui.dialog({
                id: "iconpicker-dialog",
                title: "Choose Icon",
                content: content,
                width: "350px"
            })

            function do_pick_icon(icon_topic) {
                return function() {
                    // update model
                    picked_icon = icon_topic
                    // update view
                    iconpicker_dialog.close()
                    image.attr({src: icon_topic.value, title: icon_topic.value})
                }
            }
        }
    }
})
