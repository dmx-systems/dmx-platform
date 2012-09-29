/**
 * DeepaMehta-specific rendering functions.
 */
function RenderHelper() {

    /**
     * @param   page_model      a TopicRenderer.PageModel object or a string.
     * @param   parent_element  Optional: the parent element the label is rendered to.
     *                          If not specified the label is rendered directly to the page panel.
     */
    this.field_label = function(page_model, parent_element, result_set) {
        parent_element = parent_element || $("#page-content")
        //
        if (typeof(page_model) == "string") {
            var label = page_model
        } else {
            var label = page_model.label
        }
        //
        if (result_set) {
            var c = result_set.items.length
            var tc = result_set.total_count
            label += " (" + c + (tc > c ? " of " + tc : "") + ")"
        }
        //
        parent_element.append($("<div>").addClass("field-label").text(label))
    }

    // ---

    /**
     * @param   topics          Topics to render (array of Topic objects).
     *
     * @param   click_handler   Optional: by supplying a click handler the caller can customize the behavoir performed
     *                          when the user clicks a topic link. The click handler is a function which receives a
     *                          topic argument.
     *                          If no click handler is specified the default handler is used. The default handler
     *                          reveals the clicked topic (by calling dm4c.do_reveal_related_topic())
     *
     * @return  The rendered topic list (a jQuery object)
     */
    this.topic_list = function(topics, click_handler, render_handler) {
        var table = $("<table>").addClass("topic-list")
        for (var i = 0, topic; topic = topics[i]; i++) {
            // render supplement text
            if (render_handler) {
                var supplement_text = render_handler(topic)
                var supplement = $("<div>").addClass("supplement-text").append(supplement_text)
            }
            // render topic
            var handler = create_click_handler(topic)
            table.append($("<tr>")
                .append($("<td>").append(this.icon_link(topic, handler)))
                .append($("<td>").append(this.topic_link(topic, handler)).append(supplement))
            )
        }
        return table

        function create_click_handler(topic) {
            return function() {
                if (click_handler) {
                    click_handler(topic)
                } else {
                    dm4c.do_reveal_related_topic(topic.id)
                }
                return false    // suppress browser's own link-click behavoir
            }
        }
    }

    /**
     * @param   topic       Topic to render (a Topic object).
     */
    this.topic_link = function(topic, handler) {
        var title = dm4c.type_label(topic.type_uri)
        var text = this.link_text(topic)
        return $("<a>").attr({href: "#", title: title}).append(text).click(handler)
    }

    this.icon_link = function(topic, handler) {
        return this.type_icon(topic.type_uri).click(handler)
    }

    this.link_text = function(topic) {
        if (dm4c.get_topic_type(topic.type_uri).data_type_uri == "dm4.core.html") {
            var text = js.strip_html(topic.value)
        } else {
            var text = topic.value.toString()   // value can be a number or a boolean as well (truncate would fail)
        }
        return js.truncate(text, dm4c.MAX_TOPIC_LINK_CHARS)
    }

    /**
     * @return  The <img> element (jQuery object).
     */
    this.type_icon = function(type_uri) {
        var src   = dm4c.get_icon_src(type_uri)
        var title = dm4c.type_label(type_uri)
        return this.icon(src, title)
    }

    this.icon = function(src, title) {
        title = title || src
        return $("<img>").attr({src: src, title: title}).addClass("type-icon")
    }

    // ---

    /**
     * @param   page_model      Optional: the initial value (a TopicRenderer.PageModel object or a non-object value).
     *                          If not specified the text field will be empty.
     *
     * @return  The <input> element (jQuery object).
     */
    this.input = function(page_model, size) {
        if (typeof(page_model) == "object") {
            var value = page_model.value
        } else {
            var value = page_model
        }
        // Note: we use an object argument for attr().
        // attr("value", value) would be interpreted as 1-argument attr() call if value is undefined.
        return $('<input type="text">').attr({value: value, size: size})
    }

    /**
     * @param   page_model      a boolean value or a TopicRenderer.PageModel of a boolean topic.
     */
    this.checkbox = function(page_model) {
        var checkbox = $("<input type='checkbox'>")
        if (typeof(page_model) == "boolean") {
            var checked = page_model
        } else {
            var checked = page_model.value
        }
        if (checked) {
            checkbox.attr("checked", "checked")
        }
        return checkbox
    }

    // ---

    /**
     * Renders a menu which is populated by all topics of the specified type.
     * The menu item values can be either the respective topic IDs or the topic URIs.
     *
     * @param   selected_id_or_uri  The ID or the URI of the initially selected item.
     *                              If an ID (number) is specified the menu item values are the respective topic IDs.
     *                              If an URI (string) is specified the menu item values are the respective topic URIs.
     *
     * @return  a GUIToolkit Menu object
     */
    this.topic_menu = function(topic_type_uri, selected_id_or_uri) {
        // determine item value type
        if (typeof selected_id_or_uri == "number") {
            var value_attr = "id"
        } else if (typeof selected_id_or_uri == "string") {
            var value_attr = "uri"
        } else {
            throw "RendererHelperError: topic_menu(): illegal \"selected_id_or_uri\" argument"
        }
        // fetch all instances
        var topics = dm4c.restc.get_topics(topic_type_uri, false, true).items   // fetch_composite=false, sort=true
        // build menu
        var menu = dm4c.ui.menu()
        for (var i = 0, topic; topic = topics[i]; i++) {
            menu.add_item({label: topic.value, value: topic[value_attr]})
        }
        menu.select(selected_id_or_uri)
        //
        return menu
    }



    // === Direct-to-page Rendering ===

    this.topic_associations = function(topic_id) {
        var result = dm4c.restc.get_topic_related_topics(topic_id, undefined, true, dm4c.MAX_RESULT_SIZE)
                                                                // traversal_filter=undefined, sort=true
        this.field_label("Associations", undefined, result)     // parent_element=undefined
        this.page(this.topic_list(result.items))
    }

    this.association_associations = function(assoc_id) {
        var result = dm4c.restc.get_association_related_topics(assoc_id, undefined, true, dm4c.MAX_RESULT_SIZE)
                                                                // traversal_filter=undefined, sort=true
        this.field_label("Associations", undefined, result)     // parent_element=undefined
        this.page(this.topic_list(result.items, function(topic) {
            dm4c.show_topic(dm4c.fetch_topic(topic.id), "show", undefined, true)    // coordinates=undefined,
        }))                                                                         // do_center=true
    }

    // ---

    this.page = function(html) {
        $("#page-content").append(html)
    }
}
