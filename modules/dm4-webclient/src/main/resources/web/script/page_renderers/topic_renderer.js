/**
 * A page renderer that models a page as a set of fields.
 *
 * @see     PageRenderer interface (script/interfaces/page_renderer.js).
 */
function TopicRenderer() {

    var page_model  // either a TopicRenderer.FieldModel object (non-composite) or an object (composite):
                    //     key: assoc def URI
                    //     value: either a TopicRenderer.FieldModel object or again a page model object

    // The autocomplete list
    $("#page-panel").append($("<div>").addClass("autocomplete-list"))
    autocomplete_item = -1

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {
        page_model = create_page_model(topic, "viewable")
        // trigger hook
        dm4c.trigger_plugin_hook("pre_render_page", topic, page_model)
        //
        render_page_model(page_model, "render_field")
        //
        dm4c.render.associations(topic.id)
    }

    this.render_form = function(topic) {
        page_model = create_page_model(topic, "editable")
        // trigger hook
        dm4c.trigger_plugin_hook("pre_render_form", topic, page_model)
        //
        render_page_model(page_model, "render_form_element")
    }

    this.process_form = function(topic) {
        dm4c.do_update_topic(topic, build_topic_model(page_model))
    }



    // ******************
    // *** Public API ***
    // ******************



    this.get_page_model = function() {
        return page_model
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    // === Page Model ===

    /**
     * @param   setting     "viewable" or "editable"
     */
    function create_page_model(topic, setting) {
        return TopicRenderer.create_page_model(topic, undefined, "", topic, setting)
    }

    /**
     * @param   page_model  the page model to render. If undefined nothing is rendered.
     */
    function render_page_model(page_model, render_func_name) {
        TopicRenderer.render_page_model(page_model, render_func_name, 0, $("#page-content"))
    }

    /**
     * Reads out values from a page model's GUI elements and builds a topic model from it.
     *
     * @return  a topic model (object), a topic reference (string), a simple topic value, or null.
     *          Note 1: null is returned if a simple topic is being edited and the field renderer prevents the field
     *          from being updated.
     *          Note 2: despite at deeper recursion levels this method might return a topic reference (string), or
     *          a simple topic value, the top-level call will always return a topic model (object), or null.
     *          This is because topic references are only contained in composite topic models. A simple topic model
     *          on the other hand never represents a topic reference.
     */
    function build_topic_model(page_model) {
        if (page_model instanceof TopicRenderer.FieldModel) {
            var value = page_model.read_form_value()
            // Note: undefined form value is an error (means: field renderer returned no value).
            // null is a valid form value (means: field renderer prevents the field from being updated).
            if (value == null) {
                return null
            }
            // ### TODO: explain. Compare to TextFieldRenderer.render_form_element()
            switch (page_model.assoc_def && page_model.assoc_def.assoc_type_uri) {
            case undefined:
            case "dm4.core.composition_def":
                page_model.topic.value = value
                return page_model.topic
            case "dm4.core.aggregation_def":
                return value
            default:
                throw "TopicRendererError: \"" + page_model.assoc_def.assoc_type_uri +
                    "\" is an unexpected assoc type URI"
            }
        } else if (page_model instanceof TopicRenderer.PageModel) {
            var composite = {}
            for (var assoc_def_uri in page_model.childs) {
                var child_model = page_model.childs[assoc_def_uri]
                if (js.is_array(child_model)) {
                    // cardinality "many"
                    composite[assoc_def_uri] = []
                    for (var i = 0; i < child_model.length; i++) {
                        var value = build_topic_model(child_model[i])
                        if (value != null) {
                            composite[assoc_def_uri].push(value)
                        }
                    }
                } else {
                    // cardinality "one"
                    var value = build_topic_model(child_model)
                    if (value != null) {
                        composite[assoc_def_uri] = value
                    }
                }
            }
            page_model.topic.composite = composite
            return page_model.topic
        } else {
            throw "TopicRendererError: invalid page model "
        }
    }



    // === Auto-Completion ===
    // ### FIXME: this code is inactive

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



TopicRenderer.PageModel = function(topic, assoc_def, field_uri, toplevel_topic) {

    this.topic = topic
    this.assoc_def = assoc_def
    this.uri = field_uri
    this.toplevel_topic = toplevel_topic
    this.topic_type = dm4c.get_topic_type(topic.type_uri)  // ### TODO: Topics in composite would allow topic.get_type()
    this.childs = {}

    /**
     * @param   child_model     A FieldModel, or a PageModel, or an array of FieldModels or PageModels.
     */
    this.add_child = function(assoc_def_uri, child_model) {
        this.childs[assoc_def_uri] = child_model
    }
}

/**
 * @param   topic           The topic underlying this field. Its "value" is rendered through this field model.
 *                          A topic "id" -1 indicates a topic to be created.
 * @param   assoc_def       The direct association definition that leads to this field.
 *                          For a non-composite topic it is <code>undefined</code>.
 *                          The association definition has 2 meanings:
 *                              1) its view configuration has precedence over the topic type's view configuration
 *                              2) The particular field renderers are free to operate on it.
 *                                 Field renderers which do so:
 *                                  - TextFieldRenderer (Webclient module)
 * @param   field_uri       The field URI. Unique within the page/form. The field URI is a path composed of association
 *                          definition URIs that leads to this field, e.g. "/dm4.contacts.address/dm4.contacts.street".
 *                          For a non-composite topic the field URI is an empty string.
 *                          This URI is passed to the field renderer constructors (as a property of the "field"
 *                          argument). The particular field renderers are free to operate on it. Field renderers
 *                          which do so:
 *                              - HTMLFieldRenderer (Webclient module)
 *                              - IconFieldRenderer (Icon Picker module)
 * @param   toplevel_topic  The topic the page/form is rendered for. Usually that is the selected topic.
 *                          (So, that is the same topic for all the FieldModel objects making up one page/form.)
 *                          This topic is passed to the field renderer constructors.
 *                          The particular field renderers are free to operate on it. Field renderers which do so:
 *                              - SearchResultRenderer  (Webclient module)
 *                              - FileContentRenderer   (Files module)
 *                              - FolderContentRenderer (Files module)
 */
TopicRenderer.FieldModel = function(topic, assoc_def, field_uri, toplevel_topic) {

    var self = this
    this.topic = topic
    this.assoc_def = assoc_def
    this.uri = field_uri
    this.toplevel_topic = toplevel_topic
    this.value = topic.value
    this.topic_type = dm4c.get_topic_type(topic.type_uri)  // ### TODO: Topics in composite would allow topic.get_type()
    this.label = this.topic_type.value
    this.rows = get_view_config("rows")
    var field_renderer = create_field_renderer()

    this.render_field = function(parent_element) {
        trigger_renderer_hook("render_field", parent_element)
    }

    this.render_form_element = function(parent_element) {
        dm4c.render.field_label(this, parent_element)
        trigger_renderer_hook("render_form_element", parent_element)
    }

    this.read_form_value = function() {
        var form_value = trigger_renderer_hook("read_form_value")
        // Note: undefined value is an error (means: field renderer returned no value).
        // null is a valid result (means: field renderer prevents the field from being updated).
        if (form_value === undefined) {
            throw "TopicRendererError: the \"" + this.label + "\" field renderer returned no value " +
                "(js_field_renderer_class=\"" + js_field_renderer_class + "\")"
        }
        //
        return form_value
    }

    // ---

    function create_field_renderer() {
        var js_field_renderer_class = get_view_config("js_field_renderer_class")
        // error check
        if (!js_field_renderer_class) {
            throw "TopicRendererError: unknown renderer class for field \"" + self.label +
                "\" (field_uri=\"" + self.uri + "\")"
        }
        //
        return js.new_object(js_field_renderer_class, self)
    }

    /**
     * Triggers a field renderer hook.
     *
     * @param   hook_name   Name of the field renderer hook to trigger.
     * @param   <varargs>   Variable number of arguments. Passed to the hook.
     */
    function trigger_renderer_hook(hook_name) {
        // Trigger the hook only if it is defined (a field renderer must not define all hooks).
        if (field_renderer[hook_name]) {
            // ### FIXME: use apply() instead of limiting arguments
            if (arguments.length == 1) {
                return field_renderer[hook_name]()
            } else if (arguments.length == 2) {
                return field_renderer[hook_name](arguments[1])
            }
        }
    }

    function get_view_config(setting) {
        // the assoc def's config has precedence
        if (assoc_def) {
            var value = dm4c.get_view_config(assoc_def, setting)
            // Note 1: we explicitely compare to undefined to let assoc defs override with falsish values.
            // Note 2: we must regard an empty string as "not set" to not loose the default rendering classes.
            if (value !== undefined && value !== "") {      // compare to get_view_config() in webclient.js
                // regard the assoc def's value as set
                return value
            }
        }
        return dm4c.get_view_config(self.topic_type, setting, true)
    }
}

/**
 * Creates a page model for a topic.
 *
 * @param   topic           The topic the page model is created for.
 * @param   assoc_def       The association definition that leads to that topic. Undefined for the top-level call.
 *                          For a simple topic the association definition is used to create the corresponding
 *                          field model. For a complex topic the association definition is (currently) not used.
 * @param   field_uri       The (base) URI for the field model(s) to create.
 * @param   toplevel_topic  The topic the page/form is rendered for. Usually that is the selected topic.
 *                          For a simple topic the top-level topic is used to create the corresponding field model.
 *                          For a complex topic the top-level topic is just passed recursively.
 *                          Note: for the top-level call "toplevel_topic" and "topic" are usually the same.
 * @param   setting         "viewable" or "editable" (string).
 *
 * @return  The created page model, or undefined. Undefined is returned if the topic is not viewable/editable.
 *          A page model is either a FieldModel object (for a simple topic) or a PageModel object (for a complex topic).
 *          A PageModel object is a nested structure of FieldModels and (again) PageModels.
 */
TopicRenderer.create_page_model = function(topic, assoc_def, field_uri, toplevel_topic, setting) {
    var topic_type = dm4c.get_topic_type(topic.type_uri)   // ### TODO: Topics in composite would allow topic.get_type()
    if (!get_view_config()) {
        return
    }
    if (topic_type.is_simple()) {
        return new TopicRenderer.FieldModel(topic, assoc_def, field_uri, toplevel_topic)
    } else {
        var page_model = new TopicRenderer.PageModel(topic, assoc_def, field_uri, toplevel_topic)
        for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
            var child_topic_type = dm4c.get_topic_type(assoc_def.part_topic_type_uri)
            var child_field_uri = field_uri + dm4c.COMPOSITE_PATH_SEPARATOR + assoc_def.uri
            var cardinality_uri = assoc_def.part_cardinality_uri
            if (cardinality_uri == "dm4.core.one") {
                var child_topic = topic.composite[assoc_def.uri] || dm4c.empty_topic(child_topic_type.uri)
                var child_fields = TopicRenderer.create_page_model(child_topic, assoc_def, child_field_uri,
                    toplevel_topic, setting)
            } else if (cardinality_uri == "dm4.core.many") {
                var child_topics = topic.composite[assoc_def.uri] || []     // ### TODO: server: don't send empty arrays
                if (!js.is_array(child_topics)) {
                    throw "TopicRendererError: field \"" + assoc_def.uri + "\" is defined as multi-value but " +
                        "appears as single-value in " + JSON.stringify(topic)
                }
                if (child_topics.length == 0) {
                    child_topics.push(dm4c.empty_topic(child_topic_type.uri))
                }
                var child_fields = []
                for (var j = 0, child_topic; child_topic = child_topics[j]; j++) {
                    var child_field = TopicRenderer.create_page_model(child_topic, assoc_def,
                        child_field_uri, toplevel_topic, setting)
                    child_fields.push(child_field)
                }
            } else {
                throw "TopicRendererError: \"" + cardinality_uri + "\" is an unexpected cardinality URI"
            }
            if (child_fields) {
                page_model.add_child(assoc_def.uri, child_fields)
            }
        }
        return page_model;
    }

    // compare to get_view_config() in TopicRenderer.FieldModel
    function get_view_config() {
        // the assoc def's config has precedence
        if (assoc_def) {
            var value = dm4c.get_view_config(assoc_def, setting)
            if (value != undefined) {
                return value
            }
        }
        return dm4c.get_view_config(topic_type, setting, true)
    }
}

/**
 * @param   incremental     Boolean
 */
TopicRenderer.render_page_model = function(page_model, render_func_name, level, ref_element, incremental) {
    // Note: if the topic is not viewable/editable the page model is undefined
    if (!page_model) {
        return
    }
    //
    if (page_model instanceof TopicRenderer.FieldModel) {
        var box = render_box(true)      // is_simple=true
        page_model[render_func_name](box)
    } else if (page_model instanceof TopicRenderer.PageModel) {
        var box = render_box(false)     // is_simple=false
        for (var assoc_def_uri in page_model.childs) {
            var child_model = page_model.childs[assoc_def_uri]
            if (js.is_array(child_model)) {
                // cardinality "many"
                for (var i = 0; i < child_model.length; i++) {
                    TopicRenderer.render_page_model(child_model[i], render_func_name, level + 1, box)
                }
                // ### FIXME: we should not rely on function name
                if (render_func_name == "render_form_element") {
                    render_add_button(child_model, level + 1, box)
                }
            } else {
                // cardinality "one"
                TopicRenderer.render_page_model(child_model, render_func_name, level + 1, box)
            }
        }
    } else {
        throw "TopicRendererError: invalid page model"
    }

    function render_box(is_simple) {
        var box = $("<div>")
        if (!is_simple) {
            box.addClass("box").addClass("level" + level)
        }
        if (incremental) {
            ref_element.before(box)
        } else {
            ref_element.append(box)
        }
        return box
    }

    function render_add_button(page_model, level, parent_element) {
        var topic_type = page_model[0].topic_type
        var add_button = dm4c.ui.button(do_add, "Add " + topic_type.value)
        var add_button_div = $("<div>").addClass("add-button").append(add_button)
        parent_element.append(add_button_div)

        function do_add() {
            // extend page model
            var topic = dm4c.empty_topic(topic_type.uri)
            var assoc_def      = page_model[0].assoc_def
            var field_uri      = page_model[0].uri
            var toplevel_topic = page_model[0].toplevel_topic
            var _page_model = TopicRenderer.create_page_model(topic, assoc_def, field_uri, toplevel_topic, "editable")
            page_model.push(_page_model)
            // render page model
            TopicRenderer.render_page_model(_page_model, "render_form_element", level, add_button_div, true)
                                                                                                   // incremental=true
        }
    }
}
