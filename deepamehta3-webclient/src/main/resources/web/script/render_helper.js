function RenderHelper() {

    /**
     * @param   topics      Topics to render (array of Topic objects).
     */
    this.topic_list = function(topics) {
        var table = $("<table>")
        for (var i = 0, topic; topic = topics[i]; i++) {
            // icon
            var icon_td = $("<td>").addClass("topic-icon").addClass(i == topics.length - 1 ? "last-topic" : undefined)
            icon_td.append(this.topic_link(topic, this.type_icon(topic.type_uri, "type-icon")))
            // label
            var topic_td = $("<td>").addClass("topic-label").addClass(i == topics.length - 1 ? "last-topic" : undefined)
            var list_item = $("<div>").append(this.topic_link(topic, topic.value))
            dm3c.trigger_plugin_hook("render_topic_list_item", topic, list_item)
            topic_td.append(list_item)
            //
            table.append($("<tr>").append(icon_td).append(topic_td))
        }
        return table
    }

    /**
     * @param   topic       Topic to render (a Topic object).
     */
    this.topic_link = function(topic, link_content) {
        var title = dm3c.type_label(topic.type_uri)
        return $("<a>").attr({href: "#", title: title}).append(link_content).click(function() {
            dm3c.reveal_related_topic(topic.id)
            return false
        })
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    this.type_icon = function(type_uri, css_class) {
        return this.image(dm3c.get_icon_src(type_uri), css_class)
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    this.image = function(src, css_class) {
        return $("<img>").attr("src", src).addClass(css_class)
    }

    /**
     * @param   field   a Field object or a string.
     */
    this.input = function(field) {
        var value
        if (typeof(field) == "string") {
            value = field
        } else {
            value = field.value
        }
        return $("<input>").attr({type: "text", value: value})
    }

    // ---

    /**
     * @param   field   a Field object or a string.
     */
    this.field_label = function(field, suffix) {
        var label
        if (typeof(field) == "string") {
            label = field
        } else {
            label = field.label
            if (suffix) {
                label += suffix
            }
        }
        $("#page-content").append($("<div>").addClass("field-label").text(label))
    }

    this.field_value = function(value) {
        $("#page-content").append($("<div>").addClass("field-value").append(value))
    }
}
