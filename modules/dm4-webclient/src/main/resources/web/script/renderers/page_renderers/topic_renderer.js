/**
 * A page renderer that models a page as a set of fields.
 *
 * @see     PageRenderer interface (script/interfaces/page_renderer.js).
 */
(function() {

    var topic_renderer = {

        // === Page Renderer Implementation ===

        render_page: function(topic) {
            var page_model = create_page_model(topic, this.mode.INFO)
            // trigger hook
            dm4c.trigger_plugin_hook("pre_render_page", topic, page_model)
            //
            render_page_model(page_model, this.mode.INFO)
            //
            dm4c.render.topic_associations(topic.id)
        },

        render_form: function(topic) {
            var page_model = create_page_model(topic, this.mode.FORM)
            // trigger hook
            dm4c.trigger_plugin_hook("pre_render_form", topic, page_model)
            //
            render_page_model(page_model, this.mode.FORM)
            //
            return function() {
                dm4c.do_update_topic(topic_renderer.build_topic_model(page_model))
            }
        },

        // === Proprietary objects and methods ===

        mode: {
            INFO: {
                render_setting: "viewable",
                render_func_name_simple: "render_info_simple",
                render_func_name_multi:  "render_info_multi"
            },
            FORM: {
                render_setting: "editable",
                render_func_name_simple: "render_form_simple",
                render_func_name_multi:  "render_form_multi"
            }
        },

        /**
         * Creates a page model for a topic.
         *
         * A page model comprises all the information required to render a topic in the page panel (a.k.a. detail
         * panel).
         *
         * A page model is represented by a hierarchical structure of PageModel objects.
         *
         * @param   topic           The topic the page model is created for.
         * @param   assoc_def       The association definition that leads to that topic. Undefined for the top-level
         *                          call. The association definition is used to create the PageModel object.
         *                          See there for further documentation.
         * @param   field_uri       The (base) URI for the (child) model(s) to create (string). Empty ("") for the
         *                          top-level call. The field URI is used to create the child PageModel objects.
         *                          See there for further documentation.
         * @param   toplevel_topic  The topic the page/form is rendered for. Usually that is the selected topic.
         *                          For a simple topic the top-level topic is used to create the corresponding field
         *                          model. For a complex topic the top-level topic is just passed recursively.
         *                          ### FIXDOC
         *                          Note: for the top-level call "toplevel_topic" and "topic" are usually the same.
         * @param   render_mode     this.mode.INFO or this.mode.FORM (object).
         *
         * @return  The created page model, or undefined. Undefined is returned if the topic is a simple one and is not
         *          viewable/editable.
         */
        create_page_model: function(topic, assoc_def, field_uri, toplevel_topic, render_mode) {
            var topic_type = dm4c.get_topic_type(topic.type_uri)   // ### TODO: real Topics would allow topic.get_type()
            if (topic_type.is_simple()) {
                //
                if (!dm4c.get_view_config(topic_type, render_mode.render_setting, assoc_def)) {
                    return
                }
                //
                return new PageModel(PageModel.SIMPLE, topic, assoc_def, field_uri, toplevel_topic)
            } else {
                var page_model = new PageModel(PageModel.COMPOSITE, topic, assoc_def, field_uri, toplevel_topic)
                for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
                    var child_topic_type = dm4c.get_topic_type(assoc_def.part_topic_type_uri)
                    //
                    if (!dm4c.get_view_config(child_topic_type, render_mode.render_setting, assoc_def)) {
                        continue
                    }
                    //
                    var child_field_uri = field_uri + dm4c.COMPOSITE_PATH_SEPARATOR + assoc_def.uri
                    var cardinality_uri = assoc_def.part_cardinality_uri
                    if (cardinality_uri == "dm4.core.one") {
                        var child_topic = topic.composite[assoc_def.uri] || dm4c.empty_topic(child_topic_type.uri)
                        var child_model = this.create_page_model(child_topic, assoc_def, child_field_uri,
                            toplevel_topic, render_mode)
                        page_model.childs[assoc_def.uri] = child_model
                    } else if (cardinality_uri == "dm4.core.many") {
                        // ### TODO: server: don't send empty arrays
                        var child_topics = topic.composite[assoc_def.uri] || []
                        if (!js.is_array(child_topics)) {
                            throw "TopicRendererError: field \"" + assoc_def.uri + "\" is defined as multi-value but " +
                                "appears as single-value in " + JSON.stringify(topic)
                        }
                        if (child_topics.length == 0) {
                            child_topics.push(dm4c.empty_topic(child_topic_type.uri))
                        }
                        var child_model = new PageModel(PageModel.MULTI, child_topics[0], assoc_def, field_uri,
                            toplevel_topic)
                        for (var j = 0, child_topic; child_topic = child_topics[j]; j++) {
                            var child_field = this.create_page_model(child_topic, assoc_def, child_field_uri,
                                toplevel_topic, render_mode)
                            child_model.values.push(child_field)
                        }
                        page_model.childs[assoc_def.uri] = child_model
                    } else {
                        throw "TopicRendererError: \"" + cardinality_uri + "\" is an unexpected cardinality URI"
                    }
                }
                return page_model;
            }
        },

        /**
         * Renders a page model. Called recursively.
         *
         * @param   page_model      The page model to render (a PageModel object).
         * @param   render_mode     this.mode.INFO or this.mode.FORM (object).
         * @param   level           The nesting level (integer). Starts at 0.
         * @param   ref_element     The page element the rendering is attached to (a jQuery object).
         *                          Precondition: this element is already part of the DOM.
         * @param   incremental     (boolean).
         */
        render_page_model: function(page_model, render_mode, level, ref_element, incremental) {
            // Note: if the topic is not viewable/editable the page model is undefined
            if (!page_model) {
                return
            }
            //
            if (page_model.type == PageModel.SIMPLE) {
                var box = render_box(false, ref_element, incremental, remove_button_page_model())   // accentuated=false
                page_model[render_mode.render_func_name_simple](box)
            } else if (page_model.type == PageModel.COMPOSITE) {
                var box = render_box(true, ref_element, incremental, remove_button_page_model())    // accentuated=true
                for (var assoc_def_uri in page_model.childs) {
                    var child_model = page_model.childs[assoc_def_uri]
                    if (child_model.type == PageModel.MULTI) {
                        // cardinality "many"
                        var multi_box = render_box(false, box, false)                               // accentuated=false
                        child_model[render_mode.render_func_name_multi](multi_box, level + 1)
                    } else {
                        // cardinality "one"
                        this.render_page_model(child_model, render_mode, level + 1, box)
                    }
                }
            } else {
                throw "TopicRendererError: invalid page model"
            }

            function render_box(accentuated, ref_element, incremental, remove_button_page_model) {
                var box = $("<div>").addClass("box")
                // Note: a simple box doesn't get a "level" class to let it inherit the background color
                if (accentuated) {
                    box.addClass("level" + level)
                }
                if (incremental) {
                    ref_element.before(box)
                } else {
                    ref_element.append(box)
                }
                //
                if (remove_button_page_model) {
                    render_remove_button(box, remove_button_page_model)
                }
                //
                return box
            }

            // === "Remove" Button ===

            // Note: subject of removal is always a SIMPLE or a COMPOSITE, never a MULTI. So, the remove button
            // aspect is handled here at framework level. In contrast, the "Add" button is always bound to a MULTI.
            // So, the add button aspect is handled by the respective multi renderers.

            function remove_button_page_model() {
                return render_mode == topic_renderer.mode.FORM &&
                    page_model.assoc_def &&  // Note: the top-level page model has no assoc_def
                    page_model.assoc_def.part_cardinality_uri == "dm4.core.many" &&
                    page_model
            }

            /**
             * @param   parent_element  The element the remove button is appended to.
             *                          This element is removed from the page when the remove button is pressed.
             * @param   page_model      The page model of the topic to be removed when the remove button is pressed.
             */
            function render_remove_button(parent_element, page_model) {
                var remove_button = dm4c.ui.button(do_remove, undefined, "circle-minus")
                var remove_button_div = $("<div>").addClass("remove-button").append(remove_button)
                parent_element.append(remove_button_div)

                function do_remove() {
                    // update model
                    page_model.topic.delete = true
                    // update view
                    parent_element.remove()
                }
            }
        },

        /**
         * Reads out values from a page model's GUI elements and builds a topic model from it.
         *
         * @return  a topic model (object), a topic reference (string), a simple topic value, or null.
         *          Note 1: null is returned if a simple topic is being edited and the simple renderer prevents the
         *          field from being updated.
         *          Note 2: despite at deeper recursion levels this method might return a topic reference (string), or
         *          a simple topic value, the top-level call will always return a topic model (object), or null.
         *          This is because topic references are only contained in composite topic models. A simple topic model
         *          on the other hand never represents a topic reference.
         */
        build_topic_model: function(page_model) {
            if (page_model.type == PageModel.SIMPLE) {
                var value = page_model.read_form_value()
                // Note: undefined form value is an error (means: simple renderer returned no value).
                // null is a valid form value (means: simple renderer prevents the field from being updated).
                if (value == null) {
                    return null
                }
                // ### TODO: explain. Compare to TextRenderer.render_form()
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
            } else if (page_model.type == PageModel.COMPOSITE) {
                var composite = {}
                for (var assoc_def_uri in page_model.childs) {
                    var child_model = page_model.childs[assoc_def_uri]
                    if (child_model.type == PageModel.MULTI) {
                        // cardinality "many"
                        var values = child_model.read_form_values()
                        composite[assoc_def_uri] = values
                    } else {
                        // cardinality "one"
                        var value = this.build_topic_model(child_model)
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
    }

    dm4c.add_page_renderer("dm4.webclient.topic_renderer", topic_renderer)



    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Page Model ===

    /**
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object).
     */
    function create_page_model(topic, render_mode) {
        return topic_renderer.create_page_model(topic, undefined, "", topic, render_mode)
    }

    /**
     * @param   page_model      the page model to render. If undefined nothing is rendered.
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object).
     */
    function render_page_model(page_model, render_mode) {
        topic_renderer.render_page_model(page_model, render_mode, 0, $("#page-content"))
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * @param   topic           The topic underlying this field. Its "value" is rendered through this page model.
     *                          A topic "id" -1 indicates a topic to be created.
     * @param   assoc_def       The direct association definition that leads to this field.
     *                          For a non-composite topic it is <code>undefined</code>.
     *                          The association definition has 2 meanings:
     *                              1) its view configuration has precedence over the topic type's view configuration
     *                              2) The particular simple renderers are free to operate on it.
     *                                 Simple renderers which do so:
     *                                  - TextRenderer (Webclient module)
     * @param   field_uri       The field URI. Unique within the page/form. The field URI is a path composed of
     *                          association definition URIs that leads to this field, e.g.
     *                          "/dm4.contacts.address/dm4.contacts.street".
     *                          For a non-composite topic the field URI is an empty string.
     *                          This URI is passed to the simple renderer constructors (as a property of the "field"
     *                          argument). The particular simple renderers are free to operate on it. Simple renderers
     *                          which do so:
     *                              - HTMLRenderer (Webclient module)
     *                              - IconRenderer (Icon Picker module)
     * @param   toplevel_topic  The topic the page/form is rendered for. Usually that is the selected topic.
     *                          (So, that is the same topic for all the PageModel objects making up one page/form.)
     *                          This topic is passed to the simple renderer constructors.
     *                          The particular simple renderers are free to operate on it. Simple renderers which do so:
     *                              - SearchResultRenderer  (Webclient module)
     *                              - FileContentRenderer   (Files module)
     *                              - FolderContentRenderer (Files module)
     */
    function PageModel(type, topic, assoc_def, field_uri, toplevel_topic) {

        var self = this
        this.type = type        // page model type (SIMPLE, COMPOSITE, MULTI)
        this.topic = topic      // the SIMPLE topic, the COMPOSITE topic, or the 1st MULTI topic
        this.childs = {}        // used for COMPOSITE
        this.values = []        // used for MULTI
        this.assoc_def = assoc_def
        this.uri = field_uri
        this.toplevel_topic = toplevel_topic
        this.topic_type = dm4c.get_topic_type(topic.type_uri)   // ### TODO: real Topics would allow topic.get_type()
        this.value = topic.value
        this.label = this.topic_type.value
        this.rows = dm4c.get_view_config(self.topic_type, "rows", assoc_def)
        var renderer_uri
        var renderer = lookup_renderer()
        var form_reading_function

        // === Simple Renderer ===

        this.render_info_simple = function(parent_element) {
            renderer.render_info(this, parent_element)
        }

        this.render_form_simple = function(parent_element) {
            dm4c.render.field_label(this, parent_element)
            form_reading_function = renderer.render_form(this, parent_element)
        }

        this.read_form_value = function() {
            if (!form_reading_function) {
                throw "TopicRendererError: the renderer for \"" + this.label + "\" provides no " +
                    "form reading function (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            var form_value = form_reading_function()
            // Note: undefined value is an error (means: simple renderer returned no value).
            // null is a valid result (means: simple renderer prevents the field from being updated).
            if (form_value === undefined) {
                throw "TopicRendererError: the form reading function for \"" + this.label +
                    "\" returned no value (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            return form_value
        }

        // === Multi Renderer ===

        this.render_info_multi = function(parent_element, level) {
            renderer.render_info(this.values, parent_element, level)
        }

        this.render_form_multi = function(parent_element, level) {
            form_reading_function = renderer.render_form(this.values, parent_element, level)
        }

        this.read_form_values = function() {
            if (!form_reading_function) {
                throw "TopicRendererError: the renderer for \"" + this.label + "\" provides no " +
                    "form reading function (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            var form_values = form_reading_function()
            // Note: undefined value is an error (means: simple renderer returned no value).
            // null is a valid result (means: simple renderer prevents the field from being updated).
            if (form_values === undefined) {
                throw "TopicRendererError: the form reading function for \"" + this.label +
                    "\" returned no value (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            return form_values
        }

        // ===

        function lookup_renderer() {
            switch (type) {
            case PageModel.SIMPLE:
                renderer_uri = dm4c.get_view_config(self.topic_type, "simple_renderer_uri", assoc_def)
                return dm4c.get_simple_renderer(renderer_uri)
            case PageModel.COMPOSITE:
                // ### TODO
                return null
            case PageModel.MULTI:
                renderer_uri = dm4c.get_view_config(self.topic_type, "multi_renderer_uri", assoc_def)
                return dm4c.get_multi_renderer(renderer_uri)
            default:
                throw "TopicRendererError: \"" + type + "\" is an unknown page model type"
            }
        }
    }

    PageModel.SIMPLE = 1
    PageModel.COMPOSITE = 2
    PageModel.MULTI = 3
})()
