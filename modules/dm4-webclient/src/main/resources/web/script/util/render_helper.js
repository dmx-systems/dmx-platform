/**
 * DeepaMehta-specific rendering functions.
 */
function RenderHelper() {

    var self = this

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
     * Renders a clickable list of topics, one topic per row.
     * Each row renders the topic's icon and the topic's label.
     *
     * @param   topics          Topics to render (array of Topic objects).
     *                          Note: actually the array can contain any kind of objects as long as each object
     *                          has these properties:
     *                              "type_uri" -- required to render the icon and tooltip
     *                              "value"    -- required to render the topic link
     *                              "id"       -- only required by the default click handler
     *
     * @param   click_handler   Optional: the handler called when a topic is clicked. 2 arguments are passed:
     *                              1) the clicked topic (an object)
     *                              2) the spot where the topic has been clicked: "icon" or "label" (a string).
     *                          If no click handler is specified the default handler is used. The default handler
     *                          reveals the clicked topic by calling dm4c.do_reveal_related_topic().
     *
     * @return  The rendered topic list (a jQuery object)
     */
    this.topic_list = function(topics, click_handler, render_handler) {
        var table = $("<table>").addClass("topic-list")
        for (var i = 0, topic; topic = topics[i]; i++) {
            // don't list hidden topics, e.g. passwords
            if (dm4c.get_topic_type(topic.type_uri).is_hidden()) {
                continue
            }
            // render supplement text
            if (render_handler) {
                var supplement_text = render_handler(topic)
                var supplement = $("<div>").addClass("supplement-text").append(supplement_text)
            }
            // render topic
            var icon_handler = click_handler_for(topic, "icon")
            var label_handler = click_handler_for(topic, "label")
            var title = tooltips(topic, icon_handler == undefined)
            table.append($("<tr>")
                .append($("<td>").append(this.icon_link(topic, icon_handler, title.icon)))
                .append($("<td>").append(this.topic_link(topic, label_handler, title.label)).append(supplement))
            )
        }
        return table

        function click_handler_for(topic, spot) {
            // create no handler for the "icon" spot if the topic is already visible on canvas
            if (spot == "icon" && dm4c.topicmap_renderer.is_topic_visible(topic.id)) {
                return
            }
            //
            return function() {
                if (click_handler) {
                    click_handler(topic, spot)
                } else {
                    var action = spot == "label" && "show"
                    dm4c.do_reveal_related_topic(topic.id, action)
                }
                // reflect "visible on canvas" status in page panel
                if (spot == "icon") {
                    dm4c.page_panel.refresh()
                }
                // suppress browser's own link-click behavoir
                return false
            }
        }

        function tooltips(topic, is_visible_on_canvas) {
            var type_info = type_info()
            return is_visible_on_canvas ? {
                icon:  type_info + "\n\nAlready revealed on topicmap",
                label: type_info + "\n\nClick to focus"
            } : {
                icon:  type_info + "\n\nClick to reveal on topicmap",
                label: type_info + "\n\nClick to reveal on topicmap and focus"
            }

            function type_info() {
                return dm4c.type_label(topic.type_uri) + (topic.uri && " (" + topic.uri + ")")
            }
        }
    }

    /**
     * Renders a topic label as a link.
     *
     * @param   title       Optional: the tooltip title.
     *                      If not specified the topic's type name is used.
     */
    this.topic_link = function(topic, handler, title) {
        var text = this.link_text(topic)
        title = title || dm4c.type_label(topic.type_uri)
        return $("<a>").attr({href: "#", title: title}).append(text).click(handler)
    }

    /**
     * Renders a topic icon and optionally attaches a click handler to it.
     *
     * @param   handler     Optional: the click handler.
     *                      If not specified the icon is rendered as disabled and doesn't respond to clicks.
     * @param   title       Optional: the tooltip title.
     *                      If not specified the topic's type name is used.
     */
    this.icon_link = function(topic, handler, title) {
        var type_icon = this.type_icon(topic.type_uri, title)
        if (handler) {
            type_icon.click(handler)
        } else {
            type_icon.addClass("disabled")
        }
        return type_icon
    }

    this.link_text = function(topic) {
        if (dm4c.get_topic_type(topic.type_uri).data_type_uri == "dm4.core.html") {
            var text = js.strip_html(topic.value)
        } else {
            var text = topic.value
        }
        return js.truncate(text, dm4c.MAX_TOPIC_LINK_CHARS)
    }

    /**
     * Renders a topic type icon.
     *
     * @param   type_uri    The topic type URI.
     * @param   title       Optional: the tooltip title.
     *                      If not specified the topic type name is used.
     *
     * @return  An <img> element of CSS class "type-icon" (jQuery object).
     */
    this.type_icon = function(type_uri, title) {
        var src = dm4c.get_type_icon_src(type_uri)
        title = title || dm4c.type_label(type_uri)
        return this.icon(src, title)
    }

    this.icon = function(src, title) {
        title = title || src
        return $("<img>").attr({src: src, title: title}).addClass("type-icon")
    }

    // ---

    /**
     * Renders a link.
     *
     * @param   text        The link text.
     * @param   handler     The click handler.
     *                      Must not care about its return value. This link's click handler always returns false.
     * @param   title       Optional: the tooltip title.
     *                      If not specified no tooltip is shown.
     */
    this.link = function(text, handler, title) {
        return $("<a>").attr({href: "#", title: title}).append(text).click(function() {
            handler()
            return false
        })
    }

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
     * @param   handler             Optional: The callback function. Called every time the user selects a menu item.
     *                              One argument is passed: the selected menu item (an object with "value" and "label"
     *                              properties).
     *
     * @return  a GUIToolkit Menu object
     */
    this.topic_menu = function(topic_type_uri, selected_id_or_uri, handler) {
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
        var menu = dm4c.ui.menu(handler)
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
        group_topics(result.items, function(title, group) {
            self.field_label(title)
            self.page(self.topic_list(group))
        })
    }

    this.association_associations = function(assoc_id) {
        var result = dm4c.restc.get_association_related_topics(assoc_id, undefined, true, dm4c.MAX_RESULT_SIZE)
                                                                // traversal_filter=undefined, sort=true
        group_topics(result.items, function(title, group) {
            self.field_label(title)
            self.page(self.topic_list(group, function(topic, spot) {
                // Note: for associations we need a custom click handler because the default
                // one (do_reveal_related_topic()) assumes a topic as the source.
                var action = spot == "label" && "show"
                dm4c.show_topic(dm4c.fetch_topic(topic.id), action, undefined, true)    // coordinates=undefined,
            }))                                                                         // do_center=true
        })
    }

    // ---

    function group_topics(topics, callback) {
        var topic_type_uri      // topic type URI of current group  - initialized by begin_new_group()
        var topic_type_name     // topic type name of current group - initialized by begin_new_group()
        var begin               // begin index of current group     - initialized by begin_new_group()
        //
        begin_new_group(topics[0], 0)
        for (var i = 0, topic; topic = topics[i]; i++) {
            if (topic.type_uri == topic_type_uri) {
                continue
            }
            process_group()
            begin_new_group(topic, i)
        }
        // process last group
        process_group()

        function process_group() {
            var group = topics.slice(begin, i)
            var title = topic_type_name + " (" + group.length + ")"
            callback(title, group)
        }

        function begin_new_group(topic, pos) {
            topic_type_uri = topic.type_uri
            topic_type_name = dm4c.get_topic_type(topic_type_uri).value
            begin = pos
        }
    }

    // ---

    this.page = function(html) {
        $("#page-content").append(html)
    }
}
