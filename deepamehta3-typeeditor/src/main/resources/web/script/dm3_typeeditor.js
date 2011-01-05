function dm3_typeeditor() {

    dm3c.register_field_renderer("/de.deepamehta.3-typeeditor/script/field_definition_renderer.js")
    dm3c.css_stylesheet("/de.deepamehta.3-typeeditor/style/dm3-typeeditor.css")

    // The type definition used for newly created topic types
    var DEFAULT_TYPE_DEFINITION = {
        label: "Unnamed",
        uri: "de/deepamehta/core/topictype/UnnamedTopicType",
        js_renderer_class: "PlainDocument",
        fields: [
            {
                label: "Name",
                uri: "de/deepamehta/core/property/Name",
                data_type: "text",
                editor: "single line",
                js_renderer_class: "TitleRenderer",
                indexing_mode: "FULLTEXT"
            },
            {
                label: "Description",
                uri: "de/deepamehta/core/property/Description",
                data_type: "html",
                js_renderer_class: "BodyTextRenderer",
                indexing_mode: "FULLTEXT"
            }
        ]
    }

    // The default field definition used for newly created data fields
    var DEFAULT_DATA_FIELD = {
        label: "",
        uri: "",
        data_type: "text",
        editable: true,
        viewable: true,
        editor: "single line",
        js_renderer_class: "TextFieldRenderer",
        indexing_mode: "FULLTEXT"
    }

    // Used to create the data type menu.
    // Note: is a property to let the Field Definition Renderer access it.
    // TODO: let this table build dynamically by installed plugins.
    this.DATA_TYPES = {
        text:      {label: "Text",               js_renderer_class: "TextFieldRenderer"},
        number:    {label: "Number",             js_renderer_class: "NumberFieldRenderer"},
        date:      {label: "Date",               js_renderer_class: "DateFieldRenderer"},
        html:      {label: "Styled Text (HTML)", js_renderer_class: "HTMLFieldRenderer"},
        reference: {label: "Reference",          js_renderer_class: "ReferenceFieldRenderer"}
    }

    var plugin = this

    var field_editors

    // ------------------------------------------------------------------------------------------------------ Public API



    // *******************************
    // *** Overriding Plugin Hooks ***
    // *******************************



    this.custom_create_topic = function(type_uri) {
        if (type_uri == "de/deepamehta/core/topictype/TopicType") {
            var topic_type = dm3c.create_topic_type(DEFAULT_TYPE_DEFINITION)
            return dm3c.restc.get_topic_by_id(topic_type.id)     // return the topic perspective of the type
        }
    }

    /**
     * Once a topic type is created we must
     * 1) Update the type cache.
     * 2) Rebuild the "Create" button's type menu.
     *
     * @param   topic   The topic just created.
     *                  Note: in case the just created topic is a type, the entire type definition is passed
     *                  (object with "uri", "fields", "label", "icon_src", and "js_renderer_class" attributes).
     */
    this.post_create_topic = function(topic) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            // 1) Update type cache
            var type_uri = topic.uri
            dm3c.type_cache.put(type_uri, topic)
            // 2) Rebuild type menu
            dm3c.recreate_type_menu("create-type-menu")
        }
    }

    /**
     * Once a topic type is updated we must
     * 1) Update the type cache.
     * 2) Rebuild the "Create" button's type menu.
     */
    this.post_update_topic = function(topic, old_properties) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            // 1) Update type cache
            // update type URI
            var old_type_uri = old_properties["de/deepamehta/core/property/TypeURI"]
            var new_type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            if (old_type_uri != new_type_uri) {
                dm3c.type_cache.set_topic_type_uri(old_type_uri, new_type_uri)
            }
            // update type label
            var old_type_label = old_properties["de/deepamehta/core/property/TypeLabel"]
            var new_type_label = topic.properties["de/deepamehta/core/property/TypeLabel"]
            if (old_type_label != new_type_label) {
                dm3c.type_cache.set_topic_type_label(new_type_uri, new_type_label)
            }
            // 2) Rebuild type menu
            dm3c.recreate_type_menu("create-type-menu")
        }
    }

    this.post_delete_topic = function(topic) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            // 1) Update type cache
            var type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            dm3c.type_cache.remove(type_uri)
            // 2) Rebuild type menu
            dm3c.recreate_type_menu("create-type-menu")
        }
    }

    this.pre_render_form = function(topic) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            field_editors = []
        }
    }

    this.pre_submit_form = function(topic) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            // update type definition (add, remove, and update fields)
            var type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            dm3c.log("Updating topic type \"" + type_uri + "\" (" + field_editors.length + " data fields):")
            for (var i = 0, editor; editor = field_editors[i]; i++) {
                if (editor.field_is_new) {
                    // add field
                    dm3c.log("..... \"" + editor.field.uri + "\" => new")
                    add_data_field(editor)
                } else if (editor.field_has_changed) {
                    // update field
                    dm3c.log("..... \"" + editor.field.uri + "\" => changed")
                    update_data_field(editor)
                } else if (editor.field_is_deleted) {
                    // delete field
                    dm3c.log("..... \"" + editor.field.uri + "\" => deleted")
                    remove_data_field(editor)
                } else {
                    dm3c.log("..... \"" + editor.field.uri + "\" => dummy")
                }
            }
            //
            set_data_field_order()
            // update type definition (icon)
            var icon_src = $("[field-uri=de/deepamehta/core/property/Icon] img").attr("src")
            dm3c.type_cache.get_topic_type(topic).icon_src = icon_src
        }

        function add_data_field(editor) {
            var type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            var field = editor.get_new_field()
            // update DB
            dm3c.restc.add_data_field(type_uri, field)
            // update memory
            dm3c.type_cache.add_field(type_uri, field)
        }

        function update_data_field(editor) {
            var type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            // update memory
            var field = editor.update_field()
            dm3c.log(".......... update_data_field() => " + JSON.stringify(field))
            // update DB
            dm3c.restc.update_data_field(type_uri, field)
        }

        function remove_data_field(editor) {
            var type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            // update DB
            dm3c.restc.remove_data_field(type_uri, editor.field.uri)
            // update memory
            dm3c.type_cache.remove_field(type_uri, editor.field.uri)
        }

        function set_data_field_order() {
            var type_uri = topic.properties["de/deepamehta/core/property/TypeURI"]
            var field_uris = []
            $("#field-editors li").each(function() {
                field_uris.push(get_field_editor(this).field.uri)
            })
            // update DB
            dm3c.restc.set_data_field_order(type_uri, field_uris)
            // update memory
            dm3c.type_cache.update_data_field_order(type_uri, field_uris)
        }
    }



    // **********************
    // *** Custom Methods ***
    // **********************



    this.do_add_field = function() {
        plugin.add_field_editor(js.clone(DEFAULT_DATA_FIELD), field_editors.length)
    }

    this.add_field_editor = function(field, i) {
        var field_editor = new FieldEditor(field, i)
        field_editors.push(field_editor)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_field_editor(dom) {
        var editor_id = $(dom).attr("id").substr("field-editor_".length)
        return field_editors[editor_id]
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * A widget for editing a data field definition.
     *
     * All changes are performed on a working copy, allowing the caller to cancel the changes.
     * Keeps track of user interaction and tells the caller how to update the actual data field model eventually.
     *
     * @param   field   the data field definition to edit
     *                  (object with "uri", "data_type", "label", "indexing_mode", and "js_renderer_class" attributes)
     */
    function FieldEditor(field, editor_id) {

        dm3c.log("Creating FieldEditor for \"" + field.uri + "\" editor ID=" + editor_id)
        dm3c.log("..... " + JSON.stringify(field))
        //
        this.field = field
        // status tracking
        this.field_is_new = !field.uri      // Maximal one of these 3 flags evaluates to true.
        this.field_is_deleted = false       // Note: all flags might evaluate to false. This is the case
        this.field_has_changed = field.uri  // for newly added fields which are removed right away.
        //
        var editor = this
        var delete_button = dm3c.ui.button("deletefield-button_" + editor_id, do_delete_field, "", "close")
            .addClass("delete-field-button");
        var fieldname_input = $("<input>").attr("type", "text").val(field.label)
        var datatype_menu = create_datatype_menu()
        // - options area -
        // The options area holds data type-specific GUI elements.
        // For text fields, e.g. the text editor menu ("single line" / "multi line")
        var options = js.clone(field)       // model
        var options_area = $("<span>")      // view
        var lines_input                     // view
        //
        build_options_area()
        //
        var dom = $("<li>").attr("id", "field-editor_" + editor_id)
            .addClass("field-editor").addClass("ui-state-default")
            .append($("<span>").addClass("field-name field-editor-label").text("Name"))
            .append(delete_button).append(fieldname_input)
            .append($("<span>").addClass("field-name field-editor-label").text("Type"))
            .append(datatype_menu.dom).append(options_area)
        // add editor to page
        $("#field-editors").append(dom)
        delete_button.position({my: "right top", at: "right top", of: dom})

        this.get_new_field = function() {
            update_field()
            field.uri = js.to_id(fieldname_input.val())
            return field
        }

        this.update_field = function() {
            return update_field()
        }

        /**
         * Transfers the working copy to the actual data field model.
         */
        function update_field() {
            js.copy(options, field)
            // Note: the input fields must be read out manually
            // (for input fields the "options" model is not updated on-the-fly)
            field.label = fieldname_input.val()
            if (lines_input) {
                field.lines = lines_input.val()
            }
            //
            if (field.data_type == "reference") {
                field.editor = "checkboxes"
            }
            return field
        }

        function create_datatype_menu() {
            var menu_id = "fieldtype-menu_" + editor_id
            var menu = dm3c.ui.menu(menu_id, datatype_changed)
            menu.dom.addClass("field-editor-menu")
            // add items
            for (var data_type in plugin.DATA_TYPES) {
                menu.add_item({label: plugin.DATA_TYPES[data_type].label, value: data_type})
            }
            // select item
            menu.select(field.data_type)
            //
            return menu
        }

        function do_delete_field() {
            // update GUI
            dom.remove()
            // update model
            if (editor.field_has_changed) {
                editor.field_is_deleted = true
                editor.field_has_changed = false
            } else {
                editor.field_is_new = false
            }
        }

        function datatype_changed(menu_item) {
            var data_type = menu_item.value
            options.data_type = data_type
            // set default renderer
            options.js_renderer_class = plugin.DATA_TYPES[data_type].js_renderer_class
            // FIXME: must adjust model here, e.g. when switching from "reference" to "text" -- not nice!
            // TODO: let the adjustment do by installed plugins.
            switch (data_type) {
            case "text":
                if (options.editor == "checkboxes") {
                    options.editor = "single line"
                }
                break
            case "number":
                break
            case "date":
                break
            case "html":
                break
            case "reference":
                if (!options.ref_topic_type_uri) {
                    options.ref_topic_type_uri = dm3c.type_cache.get_type_uris()[0]
                }
                break
            default:
                alert("ERROR (FieldEditor.datatype_changed):\nunexpected data type (" + data_type + ")")
            }
            //
            update_options_area()
        }

        function update_options_area() {
            options_area.empty()
            build_options_area()
        }

        function build_options_area() {
            // TODO: let the options area build by installed plugins
            switch (options.data_type) {
            case "text":
                // text editor menu
                build_texteditor_menu()
                // lines input
                if (options.editor == "multi line") {
                    build_lines_input()
                }
                break
            case "number":
                break
            case "date":
                break
            case "html":
                build_lines_input()
                break
            case "reference":
                build_topictype_menu()
                break
            default:
                alert("ERROR (FieldEditor.build_options_area):\nunexpected data type (" + options.data_type + ")")
            }

            function build_texteditor_menu() {
                var texteditor_menu = dm3c.ui.menu("texteditor-menu_" + editor_id, texteditor_changed)
                texteditor_menu.dom.addClass("field-editor-menu")
                texteditor_menu.add_item({label: "Single Line", value: "single line"})
                texteditor_menu.add_item({label: "Multi Line", value: "multi line"})
                texteditor_menu.select(options.editor)
                //
                options_area.append(texteditor_menu.dom)

                function texteditor_changed(menu_item) {
                    options.editor = menu_item.value
                    update_options_area()
                }
            }

            function build_lines_input() {
                lines_input = $("<input>").addClass("field-editor-lines-input")
                lines_input.val(options.lines || DEFAULT_AREA_HEIGHT)
                //
                options_area.append($("<span>").addClass("field-name field-editor-label").text("Lines"))
                options_area.append(lines_input)
            }

            function build_topictype_menu() {
                var topictype_menu = dm3c.create_type_menu("topictype-menu_" + editor_id, topictype_changed)
                topictype_menu.select(options.ref_topic_type_uri)
                //
                options_area.append(topictype_menu.dom)

                function topictype_changed(menu_item) {
                    options.ref_topic_type_uri = menu_item.value
                }
            }
        }
    }
}
