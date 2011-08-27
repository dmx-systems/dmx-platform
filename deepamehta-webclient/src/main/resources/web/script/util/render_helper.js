function RenderHelper() {

    /**
     * @param   topics      Topics to render (array of Topic objects).
     */
    this.topic_list = function(topics) {
        var table = $("<table>")
        for (var i = 0, topic; topic = topics[i]; i++) {
            // icon
            var icon_td = $("<td>").addClass("topic-icon").addClass(i == topics.length - 1 ? "last-topic" : undefined)
            icon_td.append(this.icon_link(topic))
            // label
            var topic_td = $("<td>").addClass("topic-label").addClass(i == topics.length - 1 ? "last-topic" : undefined)
            topic_td.append(this.topic_link(topic))
            //
            table.append($("<tr>").append(icon_td).append(topic_td))
        }
        return table
    }

    /**
     * @param   topic       Topic to render (a Topic object).
     */
    this.topic_link = function(topic) {
        var title = dm4c.type_label(topic.type_uri)
        return $("<a>").attr({href: "#", title: title}).append(topic.value).click(reveal_handler(topic))
    }

    this.icon_link = function(topic) {
        return this.type_icon(topic.type_uri, "type-icon").click(reveal_handler(topic))
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
    this.type_icon = function(type_uri, css_class) {
        var src   = dm4c.get_icon_src(type_uri)
        var title = dm4c.type_label(type_uri)
        return this.image(src, title, css_class)
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    this.image = function(src, title, css_class) {
        return $("<img>").attr({src: src, title: title}).addClass(css_class)
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
        var topics = dm4c.restc.get_topics(topic_type_uri, true)    // sort=true
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
        var topics = dm4c.restc.get_related_topics(topic_id, undefined, true)   // traversal_filter=undefined, sort=true
        //
        this.field_label("Associations (" + topics.length + ")")
        this.field_value(this.topic_list(topics))
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
