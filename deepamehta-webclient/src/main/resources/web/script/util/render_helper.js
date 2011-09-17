function RenderHelper() {

    /**
     * @param   topics      Topics to render (array of Topic objects).
     */
    this.topic_list = function(topics, click_handler) {
        var table = $("<table>").addClass("topic-list")
        for (var i = 0, topic; topic = topics[i]; i++) {
            var handler = click_handler && click_handler(topic) || reveal_handler(topic)
            table.append($("<tr>")
                .append($("<td>").append(this.icon_link(topic, handler)))
                .append($("<td>").append(this.topic_link(topic, handler)))
            )
        }
        return table
    }

    /**
     * @param   topic       Topic to render (a Topic object).
     */
    this.topic_link = function(topic, handler) {
        var title = dm4c.type_label(topic.type_uri)
        return $("<a>").attr({href: "#", title: title}).append(topic.value).click(handler)
    }

    this.icon_link = function(topic, handler) {
        return this.type_icon(topic.type_uri).click(handler)
    }

    function reveal_handler(topic) {
        return function() {
            dm4c.do_reveal_related_topic(topic.id)
            return false
        }
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    this.type_icon = function(type_uri) {
        var src   = dm4c.get_icon_src(type_uri)
        var title = dm4c.type_label(type_uri)
        return $("<img>").attr({src: src, title: title}).addClass("type-icon")
    }

    // ---

    /**
     * @param   field   a Field object or a string.
     *
     * @return  The <input> element (jQuery object).
     */
    this.input = function(field) {
        var value
        if (typeof(field) == "string") {
            value = field
        } else {
            value = field.value
        }
        return $('<input type="text">').attr("value", value)
    }

    this.checkbox = function(field) {
        var dom = $('<input type="checkbox">')
        if (field.value) {
            dom.attr("checked", "checked")
        }
        return dom
    }

    // ---

    /**
     * @return  a GUIToolkit Menu object
     */
    this.topic_menu = function(topic_type_uri, selected_uri) {
        // retrieve all instances
        var topics = dm4c.restc.get_topics(topic_type_uri, true).items      // sort=true
        //
        var menu = dm4c.ui.menu()
        for (var i in topics) {
            menu.add_item({label: topics[i].value, value: topics[i].uri})
        }
        menu.select(selected_uri)
        //
        return menu
    }



    // === Direct-to-page Rendering ===

    this.associations = function(topic_id) {
        var result = dm4c.restc.get_related_topics(topic_id, undefined, true, dm4c.MAX_RESULT_SIZE)
                                                             // traversal_filter=undefined, sort=true
        this.field_label("Associations (" + result.items.length + " of " + result.total_count + ")")
        this.field_value(this.topic_list(result.items))
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
        this.page($("<div>").addClass("field-label").text(label))
    }

    this.field_value = function(value) {
        this.page($("<div>").addClass("field-value").append(value))
    }

    // ---

    this.page = function(html) {
        $("#page-content").append(html)
    }
}
