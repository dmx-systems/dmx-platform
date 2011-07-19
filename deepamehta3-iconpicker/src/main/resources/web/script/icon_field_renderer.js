function IconFieldRenderer(doc, field, rel_topics) {

    var plugin = dm4c.get_plugin("dm4_iconpicker")



    /**************************************************************************************************/
    /**************************************** Overriding Hooks ****************************************/
    /**************************************************************************************************/



    this.render_field = function() {
        // field label
        dm4c.render.field_label(field)
        // field value
        return render_topics_icon(rel_topics)
    }

    this.render_form_element = function() {
        var a = $("<a>")
            .attr({href: "#", "field-uri": field.uri, title: "Choose Icon"})
            .click(plugin.open_icon_dialog)
        return a.append(render_topics_icon(rel_topics))
    }

    this.read_form_value = function() {
        var icons = dm4c.get_doctype_impl(doc).topic_buffer[field.uri]
        var old_icon_id = icons.length && icons[0].id
        var new_icon_id = $("[field-uri=" + field.uri + "] img").attr("icon-topic-id")
        if (old_icon_id) {
            if (old_icon_id != new_icon_id) {
                // re-assign icon
                dm4c.delete_association(dm4c.restc.get_relation(doc.id, old_icon_id).id)
                dm4c.create_relation("RELATION", doc.id, new_icon_id)
            }
        } else if (new_icon_id) {
            // assign icon
            dm4c.create_relation("RELATION", doc.id, new_icon_id)
        }
        // prevent this field from being updated
        return null
    }



    /************************************************************************************************/
    /**************************************** Custom Methods ****************************************/
    /************************************************************************************************/



    function render_topics_icon(rel_topics) {
        if (!rel_topics.length) {
            return "<i>no icon</i>"
        }
        return plugin.render_icon(rel_topics[0])
    }
}
