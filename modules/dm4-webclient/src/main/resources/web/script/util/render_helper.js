function RenderHelper() {

    /**
     * @param   topics          Topics to render (array of Topic objects).
     * @param   click_handler   Optional: by using this argument the caller can customize the behavoir performed when
     *                          the user clicks a topic. The argument is a function which takes a topic and returns
     *                          the handler function to be called when the user clicks that topic.
     *                          If not specified the default handler is used. The default handler reveals the clicked
     *                          topic (by calling dm4c.do_reveal_related_topic())
     *
     * @return  The rendered topic list (a jQuery object)
     */
    this.topic_list = function(topics, click_handler, render_handler) {
        var table = $("<table>").addClass("topic-list")
        for (var i = 0, topic; topic = topics[i]; i++) {
            var handler = click_handler && click_handler(topic) || default_handler(topic)
            if (render_handler) {
                var supplement_text = render_handler(topic)
                var supplement = $("<div>").addClass("supplement-text").append(supplement_text)
            }
            table.append($("<tr>")
                .append($("<td>").append(this.icon_link(topic, handler)))
                .append($("<td>").append(this.topic_link(topic, handler)).append(supplement))
            )
        }
        return table

        function default_handler(topic) {
            return function() {
                dm4c.do_reveal_related_topic(topic.id)
                return false
            }
        }
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
     * @param   field   Optional: the initial value (a TopicRenderer.Field object or a non-object value).
     *                  If not specified the text field will be empty.
     *
     * @return  The <input> element (jQuery object).
     */
    this.input = function(field, size) {
        if (typeof(field) == "object") {
            var value = field.value
        } else {
            var value = field
        }
        // Note: we use an object argument for attr().
        // attr("value", value) would be interpreted as 1-argument attr() call if value is undefined.
        return $('<input type="text">').attr({value: value, size: size})
    }

    /**
     * @param   field   a TopicRenderer.Field object or a boolean.
     */
    this.checkbox = function(field) {
        var dom = $("<input type='checkbox'>")
        if (typeof(field) == "boolean") {
            var checked = field
        } else {
            var checked = field.value
        }
        if (checked) {
            dom.attr("checked", "checked")
        }
        return dom
    }

    // ---

    /**
     * @return  a GUIToolkit Menu object
     */
    this.topic_menu = function(topic_type_uri, selected_uri) {
        // fetch all instances
        var topics = dm4c.restc.get_topics(topic_type_uri, false, true).items   // fetch_composite=false, sort=true
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
        this.field_label("Associations", result)
        this.field_value(this.topic_list(result.items))
    }

    // ---

    /**
     * @param   field   a TopicRenderer.Field object or a string.
     */
    this.field_label = function(field, result_set) {
        if (typeof(field) == "string") {
            var label = field
        } else {
            var label = field.label
        }
        //
        if (result_set) {
            var c = result_set.items.length
            var tc = result_set.total_count
            label += " (" + c + (tc > c ? " of " + tc : "") + ")"
        }
        //
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
