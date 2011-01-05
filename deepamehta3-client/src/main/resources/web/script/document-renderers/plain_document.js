/**
 * A document renderer that models a document as a set of fields.
 */
function PlainDocument() {

    var field_renderers         // key: field URI, value: renderer object

    // Settings
    DEFAULT_AREA_HEIGHT = 15    // in lines

    // The autocomplete list
    $("#document-form").append($("<div>").addClass("autocomplete-list"))
    autocomplete_item = -1

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *********************************************************
    // *** Implementation of the document renderer interface ***
    // *********************************************************



    this.render_document = function(topic) {

        field_renderers = {}            // key: field URI, value: renderer object
        var defined_relation_topics = []

        dm3c.empty_detail_panel()
        render_fields()
        render_relations()
        render_buttons(topic, "detail-panel-show")

        function render_fields() {
            for (var i = 0, field; field = dm3c.type_cache.get(topic.type_uri).fields[i]; i++) {
                if (!field.viewable) {
                    continue
                }
                // create renderer
                if (!field.js_renderer_class) {
                    alert("WARNING (PlainDocument.render_document):\n\nField \"" + field.label +
                        "\" has no field renderer.\n\nfield=" + JSON.stringify(field))
                    continue
                }
                var rel_topics = related_topics(field)
                field_renderers[field.uri] = js.new_object(field.js_renderer_class, topic, field, rel_topics)
                // render field
                var field_value_div = $("<div>").addClass("field-value")
                var html = trigger_renderer_hook(field, "render_field", field_value_div)
                if (html !== undefined) {
                    $("#detail-panel").append(field_value_div.append(html))
                } else {
                    alert("WARNING (PlainDocument.render_document):\n\nRenderer for field \"" + field.label + "\" " +
                        "returned no field.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(field))
                }
            }

            function related_topics(field) {
                if (field.data_type == "reference") {
                    var topics = get_reference_field_content(topic.id, field)
                    defined_relation_topics = defined_relation_topics.concat(topics)
                    return topics
                }
            }
        }

        function render_relations() {
            var topics = dm3c.restc.get_related_topics(topic.id, [], [], ["SEARCH_RESULT;OUTGOING"])
            // don't render topics already rendered via "defined relations"
            js.substract(topics, defined_relation_topics, function(topic, drt) {
                return topic.id == drt.id
            })
            //
            dm3c.render.field_label("Relations (" + topics.length + ")")
            var field_value = $("<div>").addClass("field-value")
            field_value.append(dm3c.render_topic_list(topics))
            $("#detail-panel").append(field_value)
        }
    }

    this.render_form = function(topic) {

        field_renderers = {}            // key: field URI, value: renderer object
        this.topic_buffer = {}
        plain_doc = this

        dm3c.empty_detail_panel()
        dm3c.trigger_hook("pre_render_form", topic)
        render_fields()
        render_buttons(topic, "detail-panel-edit")

        function render_fields() {
            for (var i = 0, field; field = dm3c.type_cache.get(topic.type_uri).fields[i]; i++) {
                if (!field.editable) {
                    continue
                }
                // create renderer
                if (!field.js_renderer_class) {
                    alert("WARNING (PlainDocument.render_form):\n\nField \"" + field.label +
                        "\" has no field renderer.\n\nfield=" + JSON.stringify(field))
                    continue
                }
                var rel_topics = related_topics(field)
                field_renderers[field.uri] = js.new_object(field.js_renderer_class, topic, field, rel_topics)
                // render field label
                dm3c.render.field_label(field)
                // render form element
                var html = trigger_renderer_hook(field, "render_form_element")
                if (html !== undefined) {
                    $("#detail-panel").append($("<div>").addClass("field-value").append(html))
                    trigger_renderer_hook(field, "post_render_form_element")
                } else {
                    alert("WARNING (PlainDocument.render_form):\n\nRenderer for field \"" + field.label + "\" " +
                        "returned no form element.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(field))
                }
            }

            function related_topics(field) {
                if (field.data_type == "reference") {
                    var topics = get_reference_field_content(topic.id, field)
                    // buffer current topic selection to compare it at submit time
                    plain_doc.topic_buffer[field.uri] = topics
                    //
                    return topics
                }
            }
        }
    }

    this.process_form = function(topic) {
        // 1) update DB and memory
        dm3c.update_topic(topic, read_form_values())
        dm3c.trigger_hook("post_submit_form", topic)
        // 2) update GUI
        var topic_id = topic.id
        var label = dm3c.topic_label(topic)
        dm3c.canvas.set_topic_label(topic_id, label)
        dm3c.canvas.refresh()
        dm3c.render_topic()
        dm3c.trigger_hook("post_set_topic_label", topic_id, label)

        /**
         * Reads out values from GUI elements.
         */
        function read_form_values() {
            var form_values = {}
            for (var i = 0, field; field = dm3c.type_cache.get(topic.type_uri).fields[i]; i++) {
                if (!field.editable) {
                    continue
                }
                //
                var value = trigger_renderer_hook(field, "read_form_value")
                // Note: undefined value is an error (means: field renderer returned no value).
                // null is a valid result (means: field renderer prevents the field from being updated).
                if (value !== undefined) {
                    if (value != null) {
                        form_values[field.uri] = value
                    }
                } else {
                    alert("WARNING (PlainDocument.process_form):\n\nRenderer for field \"" + field.label + "\" " +
                        "returned no form value.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(field))
                }
            }
            return form_values
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    /**************/
    /*** Helper ***/
    /**************/



    function render_buttons(topic, context) {
        var commands = dm3c.get_topic_commands(topic, context)
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            var button = dm3c.ui.button(undefined, cmd.handler, cmd.label, cmd.ui_icon, cmd.is_submit)
            $("#lower-toolbar").append(button)
        }
    }

    /**
     * Returns the content of a "reference" data field.
     *
     * @return  Array of Topic objects.
     */
    function get_reference_field_content(topic_id, field) {
        // set topic type filter
        var include_topic_types = []
        if (field.ref_topic_type_uri) {
            include_topic_types.push(field.ref_topic_type_uri)
        }
        // set relation type filter
        if (field.ref_relation_type_id) {
            var include_relation_types = [field.ref_relation_type_id]
            var exclude_relation_types = []
        } else {
            var include_relation_types = []
            var exclude_relation_types = ["SEARCH_RESULT"]
        }
        //
        return dm3c.restc.get_related_topics(topic_id, include_topic_types,
                                                       include_relation_types, exclude_relation_types)
    }

    // --- Field Renderer ---

    /**
     * Triggers a renderer hook for the given field.
     *
     * @param   hook_name   Name of the renderer hook to trigger.
     * @param   <varargs>   Variable number of arguments. Passed to the hook.
     */
    function trigger_renderer_hook(field, hook_name) {
        // Lookup renderer
        var field_renderer = field_renderers[field.uri]
        // Trigger the hook only if it is defined (a renderer must not define all hooks).
        if (field_renderer[hook_name]) {
            if (arguments.length == 2) {
                return field_renderer[hook_name]()
            } else if (arguments.length == 3) {
                return field_renderer[hook_name](arguments[2])
            }
        }
    }



    /***********************/
    /*** Auto-Completion ***/
    /***********************/



    /**
     * Auto-Completion main function. Triggered for every keystroke.
     */
    this.autocomplete = function(event) {
        // dm3c.log("autocomplete: which=" + event.which)
        if (handle_special_input(event)) {
            return
        }
        // assertion
        if (this.id.substr(0, 6) != "field_") {
            alert("WARNING (PlainDocument.autocomplete):\n\nTopic " + dm3c.selected_topic.id +
                " has unexpected element id: \"" + this.id + "\".\n\nIt is expected to begin with \"field_\".")
            return
        }
        // Holds the matched items (model). These items are rendered as pulldown menu (the "autocomplete list", view).
        // Element type: array, holds all item fields as stored by the fulltext index function.
        autocomplete_items = []
        var item_id = 0
        //
        try {
            var field = get_field(this)
            var searchterm = searchterm(field, this)
            if (searchterm) {
                // --- trigger search for each fulltext index ---
                for (var i = 0, index; index = field.autocomplete_indexes[i]; i++) {
                    var result = dm3c.restc.search_topics(index, searchterm + "*")
                    //
                    if (result.rows.length && !autocomplete_items.length) {
                        show_autocomplete_list(this)
                    }
                    // --- add each result item to the autocomplete list ---
                    for (var j = 0, row; row = result.rows[j]; j++) {
                        // Note: only default field(s) is/are respected.
                        var item = row.fields["default"]
                        // Note: if the fulltext index function stores only one field per document
                        // we get it as a string, otherwise we get an array.
                        if (typeof(item) == "string") {
                            item = [item]
                        }
                        // --- Add item to model ---
                        autocomplete_items.push(item)
                        // --- Add item to view ---
                        var ac_item = dm3c.trigger_doctype_hook(dm3c.selected_topic, "render_autocomplete_item", item)
                        var a = $("<a>").attr({href: "", id: item_id++}).append(ac_item)
                        a.mousemove(item_hovered)
                        a.mousedown(process_selection)
                        // Note: we use "mousedown" instead of "click" because the click causes loosing the focus
                        // and "lost focus" is fired _before_ "mouseup" and thus "click" would never be fired.
                        // At least as long as we hide the autocompletion list on "hide focus" which we do for
                        // the sake of simplicity. This leads to non-conform GUI behavoir (action on mousedown).
                        // A more elaborated rule for hiding the autocompletion list is required.
                        $(".autocomplete-list").append(a)
                    }
                }
            }
        } catch (e) {
            alert("Error while searching: " + JSON.stringify(e))
        }
        //
        if (!autocomplete_items.length) {
            hide_autocomplete_list("no result")
        }

        function searchterm(field, input_element) {
            if (field.autocomplete_style == "item list") {
                var searchterm = current_term(input_element)
                // dm3c.log("pos=" + searchterm[1] + "cpos=" + searchterm[2] + " searchterm=\"" + searchterm[0] + "\"")
                return $.trim(searchterm[0])
            } else {
                // autocomplete_style "default"
                return input_element.value
            }
        }
    }

    function handle_special_input(event) {
        // dm3c.log("handle_special_input: event.which=" + event.which)
        if (event.which == 13) {            // return
            process_selection()
            return true
        } if (event.which == 27) {          // escape
            hide_autocomplete_list("aborted (escape)")
            return true
        } if (event.which == 38) {          // cursor up
            autocomplete_item--
            if (autocomplete_item == -2) {
                autocomplete_item = autocomplete_items.length -1
            }
            // dm3c.log("handle_special_input: cursor up, autocomplete_item=" + autocomplete_item)
            activate_list_item()
            return true
        } else if (event.which == 40) {     // cursor down
            autocomplete_item++
            if (autocomplete_item == autocomplete_items.length) {
                autocomplete_item = -1
            }
            // dm3c.log("handle_special_input: cursor down, autocomplete_item=" + autocomplete_item)
            activate_list_item()
            return true
        }
    }

    function process_selection() {
        if (autocomplete_item != -1) {
            var input_element = get_input_element()
            // trigger hook to get the item (string) to insert into the input element
            var item = dm3c.trigger_doctype_hook(dm3c.selected_topic, "process_autocomplete_selection",
                autocomplete_items[autocomplete_item])
            //
            var field = get_field(input_element)
            if (field.autocomplete_style == "item list") {
                // term[0]: the term to replace, starts immediately after the comma
                // term[1]: position of the previous comma or -1
                var term = current_term(input_element)
                var value = input_element.value
                input_element.value = value.substring(0, term[1] + 1)
                if (term[1] + 1 > 0) {
                    input_element.value += " "
                }
                input_element.value += item + ", " + value.substring(term[1] + 1 + term[0].length)
                update_viewport(input_element)
            } else {
                // autocomplete_style "default"
                input_element.value = item
            }
        }
        hide_autocomplete_list("selection performed")
    }

    function current_term(input_element) {
        var cpos = input_element.selectionStart
        var pos = input_element.value.lastIndexOf(",", cpos - 1)
        var term = input_element.value.substring(pos + 1, cpos)
        return [term, pos, cpos]
    }

    function get_input_element() {
        var input_element_id = $(".autocomplete-list").attr("id").substr(7) // 7 = "aclist_".length
        var input_element = $("#" + input_element_id).get(0)
        return input_element
    }

    function get_field(input_element) {
        var field_uri = input_element.id.substr(6)            // 6 = "field_".length
        var field = dm3c.type_cache.get_data_field(dm3c.type_cache.get(dm3c.selected_topic.type_uri), field_uri)
        return field
    }

    /**
     * Moves the viewport of the input element in a way the current cursor position is on-screen.
     * This is done by triggering the space key followed by a backspace.
     */
    function update_viewport(input_element) {
        // space
        var e = document.createEvent("KeyboardEvent");
        e.initKeyEvent("keypress", true, true, null, false, false, false, false, 0, 32);
        input_element.dispatchEvent(e);
        // backspace
        e = document.createEvent("KeyboardEvent");
        e.initKeyEvent("keypress", true, true, null, false, false, false, false, 8, 0);
        input_element.dispatchEvent(e);
    }

    this.lost_focus = function() {
        hide_autocomplete_list("lost focus")
    }

    function show_autocomplete_list(input_element) {
        var pos = $(input_element).position()
        // calculate position
        var top = pos.top + $(input_element).outerHeight()
        var left = pos.left
        // limit size (avoids document growth and thus window scrollbars)
        var max_width = window.innerWidth - left - 26   // leave buffer for vertical document scrollbar
        var max_height = window.innerHeight - top - 2
        //
        $(".autocomplete-list").attr("id", "aclist_" + input_element.id)
        $(".autocomplete-list").css({top: top, left: left})
        $(".autocomplete-list").css({"max-width": max_width, "max-height": max_height, overflow: "hidden"})
        $(".autocomplete-list").empty()
        $(".autocomplete-list").show()
    }

    function hide_autocomplete_list(msg) {
        $(".autocomplete-list").hide()
        autocomplete_item = -1
    }

    function activate_list_item() {
        $(".autocomplete-list a").removeClass("active")
        $(".autocomplete-list a:eq(" + autocomplete_item + ")").addClass("active")
    }

    function item_hovered() {
        autocomplete_item = this.id
        activate_list_item()
    }
}
