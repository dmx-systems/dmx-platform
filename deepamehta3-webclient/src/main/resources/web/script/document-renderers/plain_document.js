/**
 * A document renderer that models a document as a set of fields.
 */
function PlainDocument() {

    var fields      // either a Field object (non-composite) or an object (composite):
                    //     key: assoc def URI
                    //     value: either a Field object (non-composite) or again a fields object (composite)

    // Settings
    var DEFAULT_FIELD_ROWS = 1

    // The autocomplete list
    $("#document-form").append($("<div>").addClass("autocomplete-list"))
    autocomplete_item = -1

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *********************************************************
    // *** Implementation of the document renderer interface ***
    // *********************************************************



    this.render_document = function(topic) {

        dm3c.empty_detail_panel()
        render_fields("", dm3c.type_cache.get(topic.type_uri))
        render_relations()
        render_buttons(topic, "detail-panel-show")

        function render_fields(field_uri, topic_type, assoc_def) {
            if (topic_type.data_type_uri == "dm3.core.composite") {
                var fields = {}
                for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
                    var topic_type_2 = dm3c.type_cache.get(assoc_def.topic_type_uri_2)
                    var child_field_uri = field_uri + dm3c.COMPOSITE_PATH_SEPARATOR + assoc_def.uri
                    var child_fields = render_fields(child_field_uri, topic_type_2, assoc_def)
                    if (child_fields) {
                        fields[assoc_def.uri] = child_fields
                    }
                }
                return fields;
            } else {
                var field = new Field(field_uri, topic, topic_type, assoc_def)
                if (field.viewable) {
                    field.render_field()
                    return field
                }
            }
        }

        function render_relations() {
            var topics = dm3c.restc.get_related_topics(topic.id, "dm3.core.association")
            // render label
            dm3c.render.field_label("Associations (" + topics.length + ")")
            // render field
            var field_value_div = $("<div>").addClass("field-value")
            field_value_div.append(dm3c.render_topic_list(topics))
            $("#detail-panel").append(field_value_div)
        }
    }

    this.render_form = function(topic) {

        this.topic_buffer = {}
        plain_doc = this

        dm3c.empty_detail_panel()
        dm3c.trigger_hook("pre_render_form", topic)
        fields = render_fields("", dm3c.type_cache.get(topic.type_uri))
        render_buttons(topic, "detail-panel-edit")
        // alert("render_form(): fields=" + JSON.stringify(fields));

        function render_fields(field_uri, topic_type, assoc_def) {
            if (topic_type.data_type_uri == "dm3.core.composite") {
                var fields = {}
                for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
                    var topic_type_2 = dm3c.type_cache.get(assoc_def.topic_type_uri_2)
                    var child_field_uri = field_uri + dm3c.COMPOSITE_PATH_SEPARATOR + assoc_def.uri
                    var child_fields = render_fields(child_field_uri, topic_type_2, assoc_def)
                    if (child_fields) {
                        fields[assoc_def.uri] = child_fields
                    }
                }
                return fields;
            } else {
                var field = new Field(field_uri, topic, topic_type, assoc_def)
                if (field.editable) {
                    field.render_form_element()
                    return field
                }
            }
        }
    }

    this.process_form = function(topic) {

        // 1) update DB and memory
        topic = dm3c.update_topic(topic, build_topic_data())
        dm3c.trigger_hook("post_submit_form", topic)
        // 2) update GUI
        dm3c.canvas.update_topic(topic)
        dm3c.canvas.refresh()
        dm3c.render_topic()
        // ### dm3c.trigger_hook("post_set_topic_label", topic_id, label)

        /**
         * Reads out values from GUI elements and builds a topic data object from it.
         *
         * @return  a topic data object
         */
        function build_topic_data() {
            var topic_data = {
                id: topic.id,
                uri: topic.uri,
                type_uri: topic.type_uri
            }
            if (js.instance_of(fields, Field)) {
                var form_value = fields.read_form_value()
                // Note: undefined form value is an error (means: field renderer returned no value).
                // null is a valid form value (means: field renderer prevents the field from being updated).
                if (form_value != null) {
                    topic_data.value = form_value
                }
            } else {
                topic_data.composite = build_composite(fields)
            }
            return topic_data
        }

        function build_composite(fields) {
            var composite = {}
            for (var assoc_def_uri in fields) {
                if (js.instance_of(fields[assoc_def_uri], Field)) {
                    var form_value = fields[assoc_def_uri].read_form_value()
                    // Note: undefined form value is an error (means: field renderer returned no value).
                    // null is a valid form value (means: field renderer prevents the field from being updated).
                    if (form_value != null) {
                        composite[assoc_def_uri] = form_value
                    }
                } else {
                    composite[assoc_def_uri] = build_composite(fields[assoc_def_uri])
                }
            }
            return composite
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



    // === Field Renderering ===

    /**
     * @param   uri         The field URI. Unique within the page/form. The field URI is a path composed of association
     *                      definition URIs that leads to this field, e.g. "/dm3.contacts.address/dm3.contacts.street".
     *                      For a non-composite topic the field URI is an empty string.
     * @param   topic       The topic the page/form is rendered for.
     *                      Note: that is the same topic for the Field objects of one page/form.
     * @param   topic_type  The topic type underlying this field.
     *                      Note: in general the topic type is different for the Field objects of one page/form.
     * @param   assoc_def   The direct association definition that leads to this field.
     *                      For a non-composite topic it is <code>undefined</code>.
     */
    function Field(uri, topic, topic_type, assoc_def) {

        this.label = topic_type.value
        this.editable         = get_view_config("editable")
        this.viewable         = get_view_config("viewable")
        var js_renderer_class = get_view_config("js_renderer_class")
        this.rows             = get_view_config("rows")
        this.uri = uri
        var renderer

        this.render_field = function() {
            // create renderer
            if (!js_renderer_class) {
                alert("WARNING (PlainDocument.render_document):\n\nField \"" + uri +
                    "\" has no field renderer.\n\nfield=" + JSON.stringify(this))
                return
            }
            // ### var rel_topics = related_topics(field)
            renderer = js.new_object(js_renderer_class, topic, this /*, rel_topics */)
            // render field
            var field_value_div = $("<div>").addClass("field-value")
            var html = trigger_renderer_hook("render_field", field_value_div)
            if (html !== undefined) {
                $("#detail-panel").append(field_value_div.append(html))
            } else {
                alert("WARNING (PlainDocument.render_document):\n\nRenderer for field \"" + uri + "\" " +
                    "returned no field.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(this))
            }

            function related_topics(field) {
                if (field.data_type == "reference") {
                    var topics = get_reference_field_content(topic.id, field)
                    defined_relation_topics = defined_relation_topics.concat(topics)
                    return topics
                }
            }
        }

        this.render_form_element = function() {
            // create renderer
            if (!js_renderer_class) {
                alert("WARNING (PlainDocument.render_form):\n\nField \"" + uri +
                    "\" has no field renderer.\n\nfield=" + JSON.stringify(this))
                return
            }
            // ### var rel_topics = related_topics(field)
            renderer = js.new_object(js_renderer_class, topic, this /*, rel_topics */)
            // render field label
            dm3c.render.field_label(this)
            // render form element
            var html = trigger_renderer_hook("render_form_element")
            if (html !== undefined) {
                $("#detail-panel").append($("<div>").addClass("field-value").append(html))
                trigger_renderer_hook("post_render_form_element")
            } else {
                alert("WARNING (PlainDocument.render_form):\n\nRenderer for field \"" + uri + "\" " +
                    "returned no form element.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(this))
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

        this.read_form_value = function() {
            var form_value = trigger_renderer_hook("read_form_value")
            // Note: undefined value is an error (means: field renderer returned no value).
            // null is a valid result (means: field renderer prevents the field from being updated).
            if (form_value !== undefined) {
                return form_value
            } else {
                alert("WARNING (PlainDocument.process_form):\n\nRenderer for field \"" + uri + "\" " +
                    "returned no form value.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(this))
            }
        }

        /**
         * Triggers a renderer hook for this field.
         *
         * @param   hook_name   Name of the renderer hook to trigger.
         * @param   <varargs>   Variable number of arguments. Passed to the hook.
         */
        function trigger_renderer_hook(hook_name) {
            // Trigger the hook only if it is defined (a renderer must not define all hooks).
            if (renderer[hook_name]) {
                if (arguments.length == 1) {
                    return renderer[hook_name]()
                } else if (arguments.length == 2) {
                    return renderer[hook_name](arguments[1])
                }
            }
        }

        function get_view_config(setting) {

            var val = assoc_def && dm3c.get_view_config(assoc_def, setting) || dm3c.get_view_config(topic_type, setting)
            return val != undefined ? val : get_default_value()

            function get_default_value() {
                switch (setting) {
                case "editable":
                    return true
                case "viewable":
                    return true
                case "js_renderer_class":
                    return get_default_renderer_class()
                case "rows":
                    return DEFAULT_FIELD_ROWS
                default:
                    alert("Field.get_view_config: setting \"" + setting + "\" not implemented")
                }
            }

            function get_default_renderer_class() {
                switch (topic_type.data_type_uri) {
                case "dm3.core.text":
                    return "TextFieldRenderer"
                case "dm3.core.number":
                    return "NumberFieldRenderer"
                case "dm3.core.boolean":
                    return "TextFieldRenderer"  // TODO: boolean renderer (a checkbox)
                default:
                    alert("Field.get_view_config: data type \"" + topic_type.data_type_uri +
                        "\" has no default renderer class")
                }
            }
        }
    }



    // === Auto-Completion ===

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
