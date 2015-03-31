/**
 * DeepaMehta-specific rendering functions.
 */
function RenderHelper() {

    var self = this

    /**
     * Renders a clickable list of topics. Each list item consist of the topic's icon and the topic's label.
     *
     * @param   topics          Topics to render (array of Topic objects).
     *                          Note: actually the array can contain any kind of objects as long as each object
     *                          has these properties:
     *                              "type_uri" -- required to render the icon and tooltip
     *                              "value"    -- required to render the topic link
     *                              "id"       -- only required by the default click handler
     *                              "uri"      -- Optional. If set it is rendered in the tooltip
     *                              "assoc"    -- Optional. If set the assoc's type name is rendered beneath the topic
     *
     * @param   click_handler   Optional: the callback invoked when a topic is clicked. 2 arguments are passed:
     *                              "topic"    -- the clicked topic (object)
     *                              "spot"     -- indicates where the topic is clicked: "icon" or "label" (string)
     *                          If not specified the default handler is used. The default handler reveals the clicked
     *                          topic by calling dm4c.do_reveal_related_topic().
     *
     * @param   render_handler  Optional.
     *
     * @return  The rendered topic list (a jQuery object)
     */
    this.topic_list = function(topics, click_handler, render_handler) {
        var topic_list = $("<div>").addClass("topic-list")
        for (var i = 0, topic; topic = topics[i]; i++) {
            // don't list hidden topics, e.g. passwords
            if (dm4c.get_topic_type(topic.type_uri).is_hidden()) {
                continue
            }
            // supplement text
            if (render_handler) {
                var supplement_text = render_handler(topic)
                var supplement = $("<div>").addClass("supplement-text").append(supplement_text)
            }
            // topic
            var visible_in_topicmap = dm4c.topicmap_renderer.is_topic_visible(topic.id)
            var title = tooltips(topic, visible_in_topicmap)
            var icon_class = visible_in_topicmap && "visible-in-topicmap"
            topic_list.append($("<div>")
                .append(this.icon_link(topic,  click_handler_for(topic, "icon"),  title.icon, icon_class))
                .append(this.topic_link(topic, click_handler_for(topic, "label"), title.label))
                .append(assoc_type_label())
                .append(supplement)
            )
        }
        return topic_list

        /**
         * @param   spot    "label" or "icon".
         */
        function click_handler_for(topic, spot) {
            return function() {
                if (click_handler) {
                    click_handler(topic, spot)
                } else {
                    var action = spot == "label" && "show"
                    var assoc_type_uri = topic.assoc && topic.assoc.type_uri
                    dm4c.do_reveal_related_topic(topic.id, action, assoc_type_uri)
                }
                // reflect "visible on canvas" status in page panel
                if (spot == "icon") {
                    dm4c.page_panel.refresh()
                }
                // prevent browser's default link-click behavoir
                return false
            }
        }

        function tooltips(topic, visible_in_topicmap) {
            var type_info = type_info()
            return visible_in_topicmap ? {
                icon:  type_info + "\n\nAlready revealed on topicmap\nClick to focus",
                label: type_info + "\n\nAlready revealed on topicmap\nClick to select"
            } : {
                icon:  type_info + "\n\nClick to reveal on topicmap",
                label: type_info + "\n\nClick to reveal on topicmap and select"
            }

            function type_info() {
                var type_info = dm4c.topic_type_name(topic.type_uri)
                // Note: "abc" + undefined -> "abcundefined"
                if (topic.uri) {
                    type_info += " (" + topic.uri + ")"
                }
                return type_info
            }
        }

        function assoc_type_label() {
            if (topic.assoc) {
                return $("<div>").addClass("assoc-type-label")
                    .text("(" + dm4c.association_type_name(topic.assoc.type_uri) + ")")
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
        title = title || dm4c.topic_type_name(topic.type_uri)
        return $("<a>").attr({href: "#", title: title}).append(text).click(handler)
    }

    /**
     * Renders a topic icon which is clickable and has a tooltip.
     *
     * @param   topic       The icon for this topic is rendered.
     * @param   handler     The callback invoked when the icon is clicked (function).
     * @param   title       Optional: the text shown in the icon's tooltip (string).
     *                      If not specified the topic's type name is used.
     * @param   css_class   Optional: the CSS class(es) added to the icon (string).
     *                      Space separated if more than one.
     *                      If not specified no class is added to the icon.
     */
    this.icon_link = function(topic, handler, title, css_class) {
        return this.type_icon(topic.type_uri, title).click(handler).addClass(css_class)
    }

    this.link_text = function(topic) {
        var is_html = dm4c.get_topic_type(topic.type_uri).data_type_uri == "dm4.core.html"
        return is_html ? js.strip_html(topic.value) : topic.value
    }

    /**
     * Renders a topic type icon.
     *
     * @param   type_uri    The topic type URI.
     * @param   title       Optional: the tooltip title. If not specified the topic type name is used.
     *
     * @return  An <img> element of CSS class "icon" (jQuery object).
     */
    this.type_icon = function(type_uri, title) {
        return this.icon(dm4c.get_type_icon_src(type_uri), title || dm4c.topic_type_name(type_uri))
    }

    /**
     * Renders an icon.
     *
     * @param   src         The icon URL.
     * @param   title       Optional: the tooltip title. If not specified no tooltip will appear.
     *
     * @return  An <img> element of CSS class "icon" (jQuery object).
     */
    this.icon = function(src, title) {
        return $("<img>").attr({src: src, title: title}).addClass("icon")
    }

    // ---

    this.label = function(label) {
        return $("<div>").addClass("field-label").text(label)
    }

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
     * @param   size            Optional.
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
     * Renders an input field, a text area, or a combobox for the given page model.
     * Rendering takes place inside the given parent element.
     *
     * @param   parent_element  Rendering takes place inside this element.
     *
     * @return  an input field (a jQuery object), a text area (a jQuery object),
     *          or a combobox (a GUIToolkit Combobox object).
     */
    this.form_element = function(page_model, parent_element) {
        if (page_model.input_field_rows == 1) {
            switch (page_model.assoc_def && page_model.assoc_def.type_uri) {
            case undefined:
                // Note: for non-composite topics the field's assoc_def is undefined.
                // We treat this like a composition here.
            case "dm4.core.composition_def":
                return render_input()
            case "dm4.core.aggregation_def":
                return render_combobox()
            default:
                throw "RenderHelperError: \"" + page_model.assoc_def.type_uri + "\" is an unexpected assoc type URI"
            }
        } else {
            return render_textarea()
        }

        function render_input() {
            var input = dm4c.render.input(page_model)
            render(input)
            return input
        }

        function render_textarea() {
            var textarea = $("<textarea>").attr("rows", page_model.input_field_rows).text(page_model.value)
            render(textarea)
            return textarea
        }

        function render_combobox() {
            var topics = self.get_option_topics(page_model)
            // build combobox
            var combobox = dm4c.ui.combobox()
            for (var i in topics) {
                combobox.add_item({label: topics[i].value, value: topics[i].id})
            }
            if (page_model.object.id != -1) {
                // Note: the page model's object might be an empty topic (id=-1). Selection would fail.
                combobox.select(page_model.object.id)
            }
            //
            render(combobox.dom)
            return combobox
        }

        function render(form_element) {
            parent_element.append(form_element)
        }
    }

    /**
     * Returns a function that reads out the value of an input field, text area, or combobox.
     *
     * @param   form_element    an input field (a jQuery object), a text area (a jQuery object),
     *                          or a combobox (a GUIToolkit Combobox object).
     * @param   page_model      the page model underlying the form element.
     */
    this.form_element_function = function(form_element, page_model) {
        return function() {
            if (form_element instanceof jQuery) {
                // input field or text area
                var val = $.trim(form_element.val())
            } else {
                // combobox
                var val = form_element.get_selection()  // either a menu item (object) or the text entered,
                if (typeof(val) == "object") {                                          // trimmed (string)
                    // user selected existing topic
                    return dm4c.REF_PREFIX + val.value
                }
            }
            return check_input(val)
        }

        /**
         * Checks for empty input and possibly generates a deletion reference.
         * Checks input in Number fields for validity.
         */
        function check_input(val) {
            if (val) {
                // user entered non-empty value
                return page_model.object_type.is_number() ? check_number(val) : val
            } else {
                // user entered empty value
                var topic_id = page_model.object.id
                if (topic_id != -1) {
                    if (page_model.parent) {
                        // a child was assigned before -- delete it (composition) resp. the assignment (aggregagtion)
                        return dm4c.DEL_PREFIX + topic_id
                    } else {
                        // a top-level field (of a simple topic) was emptied -- accept that change
                        // Note: we allow empty Number fields instead of filling 0 automatically (no check_number())
                        return val
                    }
                } else {
                    // no child was assigned before -- abort (don't create empty topic)
                    return null // prevent this field from being updated
                }
            }
        }

        function check_number(val) {
            var value = Number(val)
            if (isNaN(value)) {
                alert("\"" + val + "\" is not a number.\n(field \"" + page_model.label + "\")\n\n" +
                    "The old value is restored.")
                return null     // prevent this field from being updated
            }
            return value
        }
    }

    // ---

    /**
     * Renders a menu which is populated by all topics of the specified type.
     * Either the topic IDs or the topic URIs can be used as the menu item values.
     *
     * @param   selected_id_or_uri
     *              The ID or the URI of the initially selected topic.
     *              If an ID (number) is specified the menu item values are the respective topic IDs.
     *              If an URI (string) is specified the menu item values are the respective topic URIs.
     * @param   filter_func
     *              Optional: the function to filter the topics appearing in the menu. The topic is passed.
     *              If the function returns a true-ish value the topic appears as enabled in the menu.
     *              Otherwise the topic appears as disabled in the menu.
     *              If no filter function is specified all topics appear as enabled.
     * @param   handler 
     *              Optional: the function that is called every time the user selects a menu item.
     *              One argument is passed: the selected menu item (an object with "value" and "label" properties).
     *
     * @return  a GUIToolkit Menu object
     */
    this.topic_menu = function(topic_type_uri, selected_id_or_uri, filter_func, handler) {
        // determine item value property
        if (typeof selected_id_or_uri == "number") {
            var item_value_prop = "id"
        } else if (typeof selected_id_or_uri == "string") {
            var item_value_prop = "uri"
        } else {
            throw "RendererHelperError: illegal \"selected_id_or_uri\" argument in topic_menu() call"
        }
        // fetch all instances
        var topics = dm4c.restc.get_topics(topic_type_uri, false, true).items   // include_childs=false, sort=true
        // build menu
        var menu = dm4c.ui.menu(handler)
        for (var i = 0, topic; topic = topics[i]; i++) {
            menu.add_item({
                label: topic.value,
                value: topic[item_value_prop],
                disabled: filter_func && !filter_func(topic)
            })
        }
        menu.select(selected_id_or_uri)
        //
        return menu
    }

    /**
     * Renders a combobox whose menu is populated by all topics of the specified type.
     * Either the topic IDs or the topic URIs can be used as the menu item values.
     *
     * @param   item_value_prop
     *              "id" or "uri", determines weather the topic IDs or the topic URIs are used as the menu item values.
     * @param   selected_id_or_uri
     *              The ID or the URI of the initially selected topic.
     *              If item_value_prop is "id" a topic ID must be specified.
     *              If item_value_prop is "uri" a topic URI must be specified.
     *              If no topic with that ID/URI exists in the menu an exception is thrown.
     *              Unless the specified value is null or undefined; in that case no topic is initially selected
     *              (that is the input field remains empty).
     */
    this.topic_combobox = function(topic_type_uri, item_value_prop, selected_id_or_uri) {
        // fetch all instances
        var topics = dm4c.restc.get_topics(topic_type_uri, false, true).items   // include_childs=false, sort=true
        // build combobox
        var combobox = dm4c.ui.combobox()
        for (var i = 0, topic; topic = topics[i]; i++) {
            combobox.add_item({
                label: topic.value,
                value: topic[item_value_prop]
            })
        }
        combobox.select(selected_id_or_uri)
        //
        return combobox
    }

    // ---

    this.get_option_topics = function(page_model) {
        var result = dm4c.fire_event("option_topics", page_model)
        var topic_type_uri = page_model.object_type.uri
        switch (result.length) {
        case 0:
            // fetch all instances
            return dm4c.restc.get_topics(topic_type_uri, false, true).items     // include_childs=false, sort=true
        case 1:
            return result[0]
        default:
            throw "RenderHelperError: " + result.length + " plugins are competing with " +
                "providing the option topics for \"" + topic_type_uri + "\""
        }
    }



    // === Direct-to-page Rendering ===

    this.topic_associations = function(topic_id) {
        var result = dm4c.restc.get_related_topics(topic_id, true)  // sort=true
        group_topics(result.items, function(title, group) {
            self.field_label(title)
            self.page(self.topic_list(group))
        })
    }

    this.association_associations = function(assoc_id) {
        // ### TODO: filter property topics
        var result = dm4c.restc.get_association_related_topics(assoc_id, undefined, true)  // traversal_filter=undefined
                                                                                           // sort=true
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
        parent_element.append(this.label(label))
    }

    this.page = function(html) {
        $("#page-content").append(html)
    }
}
