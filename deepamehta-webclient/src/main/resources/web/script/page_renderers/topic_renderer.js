/**
 * A page renderer that models a page as a set of fields.
 */
function TopicRenderer() {

    var page_model  // either a Field object (non-composite) or an object (composite):
                    //     key: assoc def URI
                    //     value: either a Field object or again a page model object

    // The autocomplete list
    $("#page-panel").append($("<div>").addClass("autocomplete-list"))
    autocomplete_item = -1

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {

        render_page_model(create_page_model(topic), render_field)
        dm4c.render.associations(topic.id)

        function render_field(field) {
            if (field.viewable) {
                field.render_field()
            }
        }
    }

    this.render_form = function(topic) {

        dm4c.trigger_plugin_hook("pre_render_form", topic)
        page_model = create_page_model(topic)
        render_page_model(page_model, render_field)

        function render_field(field) {
            if (field.editable) {
                field.render_form_element()
            }
        }
    }

    this.process_form = function(topic) {
        var topic_model = build_topic_model()
        // alert("update model " + JSON.stringify(topic_model))
        topic = dm4c.do_update_topic(topic, topic_model)
        dm4c.trigger_plugin_hook("post_submit_form", topic)

        /**
         * Reads out values from GUI elements and builds a topic model object from it.
         *
         * @return  a topic model object
         */
        function build_topic_model() {
            var topic_model = {
                id: topic.id,
                uri: topic.uri,
                type_uri: topic.type_uri
            }
            if (page_model instanceof TopicRenderer.Field) {
                var form_value = page_model.read_form_value()
                // Note: undefined form value is an error (means: field renderer returned no value).
                // null is a valid form value (means: field renderer prevents the field from being updated).
                if (form_value != null) {
                    topic_model.value = form_value
                }
            } else {
                topic_model.composite = build_composite(page_model)
            }
            return topic_model
        }

        function build_composite(fields) {
            var composite = {}
            for (var assoc_def_uri in fields) {
                if (fields[assoc_def_uri] instanceof TopicRenderer.Field) {
                    var form_value = fields[assoc_def_uri].read_form_value()
                    // Note: undefined form value is an error (means: field renderer returned no value).
                    // null is a valid form value (means: field renderer prevents the field from being updated).
                    if (form_value != null) {
                        if (typeof(form_value) == "object") {
                            // store reference to existing topic
                            composite[assoc_def_uri + "$id"] = form_value.topic_id
                        } else {
                            // store value for new topic
                            composite[assoc_def_uri] = form_value
                        }
                    }
                } else {
                    composite[assoc_def_uri] = build_composite(fields[assoc_def_uri])
                }
            }
            return composite
        }
    }



    /******************/
    /*** Public API ***/
    /******************/



    this.get_page_model = function() {
        return page_model
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    /**************/
    /*** Helper ***/
    /**************/



    // === Page Model ===

    function create_page_model(topic) {

        return create_fields("", topic.composite, topic.get_type())

        function create_fields(field_uri, composite, topic_type, assoc_def) {
            if (topic_type.data_type_uri == "dm4.core.composite") {
                var fields = {}
                for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
                    var part_topic_type = dm4c.type_cache.get_topic_type(assoc_def.part_topic_type_uri)
                    var child_field_uri = field_uri + dm4c.COMPOSITE_PATH_SEPARATOR + assoc_def.uri
                    var comp = composite && composite[assoc_def.uri]
                    var child_fields = create_fields(child_field_uri, comp, part_topic_type, assoc_def)
                    fields[assoc_def.uri] = child_fields
                }
                return fields;
            } else {
                var value = field_uri == "" ? topic.value : composite
                return new TopicRenderer.Field(field_uri, value, topic, topic_type, assoc_def)
            }
        }
    }

    function render_page_model(page_model, render_func) {
        if (page_model instanceof TopicRenderer.Field) {
            render_func(page_model)
        } else {
            for (var assoc_def_uri in page_model) {
                render_page_model(page_model[assoc_def_uri], render_func)
            }
        }
    }



    // === Auto-Completion ===

    /**
     * Auto-Completion main function. Triggered for every keystroke.
     */
    this.autocomplete = function(event) {
        // dm4c.log("autocomplete: which=" + event.which)
        if (handle_special_input(event)) {
            return
        }
        // error check
        if (this.id.substr(0, 6) != "field_") {
            alert("WARNING (TopicRenderer.autocomplete):\n\nTopic " + dm4c.selected_object.id +
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
                    var result = dm4c.restc.search_topics(index, searchterm + "*")
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
                        var ac_item = dm4c.trigger_page_renderer_hook(dm4c.selected_object,
                            "render_autocomplete_item", item)
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
                // dm4c.log("pos=" + searchterm[1] + "cpos=" + searchterm[2] + " searchterm=\"" + searchterm[0] + "\"")
                return $.trim(searchterm[0])
            } else {
                // autocomplete_style "default"
                return input_element.value
            }
        }
    }

    function handle_special_input(event) {
        // dm4c.log("handle_special_input: event.which=" + event.which)
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
            // dm4c.log("handle_special_input: cursor up, autocomplete_item=" + autocomplete_item)
            activate_list_item()
            return true
        } else if (event.which == 40) {     // cursor down
            autocomplete_item++
            if (autocomplete_item == autocomplete_items.length) {
                autocomplete_item = -1
            }
            // dm4c.log("handle_special_input: cursor down, autocomplete_item=" + autocomplete_item)
            activate_list_item()
            return true
        }
    }

    function process_selection() {
        if (autocomplete_item != -1) {
            var input_element = get_input_element()
            // trigger hook to get the item (string) to insert into the input element
            var item = dm4c.trigger_page_renderer_hook(dm4c.selected_object, "process_autocomplete_selection",
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
        var field = dm4c.type_cache.get_data_field(dm4c.type_cache.get(dm4c.selected_object.type_uri), field_uri)
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



/**
 * @param   uri         The field URI. Unique within the page/form. The field URI is a path composed of association
 *                      definition URIs that leads to this field, e.g. "/dm4.contacts.address/dm4.contacts.street".
 *                      For a non-composite topic the field URI is an empty string.
 * @param   value       The value to be rendered.
 *                      May be null/undefined, in this case an empty string is rendered.
 * @param   topic       The topic the page/form is rendered for.
 *                      Note: that is the same topic for all the Field objects of one page/form.
 * @param   topic_type  The topic type underlying this field.
 *                      Note: in general the topic type is different for the Field objects of one page/form.
 * @param   assoc_def   The direct association definition that leads to this field.
 *                      For a non-composite topic it is <code>undefined</code>.
 */
TopicRenderer.Field = function(uri, value, topic, topic_type, assoc_def) {

    // preference
    var DEFAULT_FIELD_ROWS = 1

    this.uri = uri
    this.value = value != null && value != undefined ? value : ""
    this.topic_type = topic_type
    this.assoc_def = assoc_def
    this.label = topic_type.value
    this.editable               = get_view_config("editable")
    this.viewable               = get_view_config("viewable")
    var js_field_renderer_class = get_view_config("js_field_renderer_class")
    this.rows                   = get_view_config("rows")
    var renderer

    this.render_field = function() {
        // error check
        if (!js_field_renderer_class) {
            alert("WARNING (TopicRenderer.render_page):\n\nField \"" + uri +
                "\" has no field renderer.\n\nfield=" + JSON.stringify(this))
            return
        }
        // create renderer
        renderer = js.new_object(js_field_renderer_class, topic, this)
        // render field
        var field_value_div = $("<div>").addClass("field-value")
        var html = trigger_renderer_hook("render_field", field_value_div)
        if (html !== undefined) {
            $("#page-content").append(field_value_div.append(html))
            trigger_renderer_hook("post_render_field")
        } else {
            alert("WARNING (TopicRenderer.render_page):\n\nRenderer for field \"" + uri + "\" " +
                "returned no field.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(this))
        }
    }

    this.render_form_element = function() {
        // error check
        if (!js_field_renderer_class) {
            alert("WARNING (TopicRenderer.render_form):\n\nField \"" + uri +
                "\" has no field renderer.\n\nfield=" + JSON.stringify(this))
            return
        }
        // create renderer
        renderer = js.new_object(js_field_renderer_class, topic, this)
        // render field label
        dm4c.render.field_label(this)
        // render form element
        var field_value_div = $("<div>").addClass("field-value")
        var html = trigger_renderer_hook("render_form_element")
        if (html !== undefined) {
            $("#page-content").append(field_value_div.append(html))
            trigger_renderer_hook("post_render_form_element")
        } else {
            alert("WARNING (TopicRenderer.render_form):\n\nRenderer for field \"" + uri + "\" " +
                "returned no form element.\n\ntopic ID=" + topic.id + "\nfield=" + JSON.stringify(this))
        }
    }

    this.read_form_value = function() {
        var form_value = trigger_renderer_hook("read_form_value")
        // Note: undefined value is an error (means: field renderer returned no value).
        // null is a valid result (means: field renderer prevents the field from being updated).
        if (form_value !== undefined) {
            return form_value
        } else {
            alert("WARNING (TopicRenderer.process_form):\n\nRenderer for field \"" + uri + "\" " +
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

        var val = assoc_def && dm4c.get_view_config(assoc_def, setting) || dm4c.get_view_config(topic_type, setting)
        return val != undefined ? val : get_default_value()

        function get_default_value() {
            switch (setting) {
            case "editable":
                return true
            case "viewable":
                return true
            case "js_field_renderer_class":
                return get_default_renderer_class()
            case "rows":
                return DEFAULT_FIELD_ROWS
            default:
                alert("Field.get_view_config: setting \"" + setting + "\" not implemented")
            }
        }

        function get_default_renderer_class() {
            switch (topic_type.data_type_uri) {
            case "dm4.core.text":
                return "TextFieldRenderer"
            case "dm4.core.html":
                return "HTMLFieldRenderer"
            case "dm4.core.number":
                return "NumberFieldRenderer"
            case "dm4.core.boolean":
                return "BooleanFieldRenderer"
            default:
                alert("Field.get_view_config: data type \"" + topic_type.data_type_uri +
                    "\" has no default renderer class")
            }
        }
    }
}
