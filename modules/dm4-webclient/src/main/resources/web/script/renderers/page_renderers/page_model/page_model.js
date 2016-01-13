dm4c.render.page_model = new function() {

    // ------------------------------------------------------------------------------------------------- Private Classes

    var self = this

    /**
     * @paran   page_model_type     PageModel.SIMPLE, PageModel.COMPOSITE, or PageModel.MULTI
     * @param   object              The object underlying this field (a Topic or an Association). Its "value" is
     *                              rendered through this page model.
     *                              If no underlying object exists in the DB this field is about to be rendered anyway
     *                              (e.g. as label and no content in render mode INFO or as label and input field in
     *                              render mode FORM). In this case an empty topic with id = -1 (as created by
     *                              dm4c.empty_topic()) is passed here.
     * @param   assoc_def           The direct association definition that leads to this field.
     *                              For a top-level page model <code>undefined</code> is passed here.
     *                              The association definition has 2 meanings:
     *                                1) its view configuration has precedence over the object type's view configuration
     *                                2) The particular simple renderers are free to operate on it.
     *                                   Simple renderers which do so:
     *                                     - TextRenderer (Webclient module)
     * @param   field_uri           The field URI to be stored in the "uri" property. Unique within the page/form.
     *                              The field URI is a path composed of child type URIs that leads to this field, e.g.
     *                              "/dm4.contacts.address_entry/dm4.contacts.address/dm4.contacts.city".
     *                              For a top-level page model an empty string is passed here.
     *                              The particular simple renderers are free to operate on it. Simple renderers which do
     *                              so:
     *                                - HTMLRenderer (Webclient module)
     *                                - IconRenderer (Icon Picker module)
     * @param   parent_page_model   The parent page model to be stored in the "parent" property.
     *                              For a top-level page model <code>undefined</code> is passed here.
     *                              Renderers are free to operate on it. Simple renderers which do so:
     *                                - SearchResultRenderer  (Webclient module)
     *                                - FileContentRenderer   (Files module)
     *                                - FolderContentRenderer (Files module)
     */
    function PageModel(page_model_type, object, assoc_def, field_uri, parent_page_model) {

        var _self = this
        this.type = page_model_type // page model type (SIMPLE, COMPOSITE, MULTI)
        this.childs = {}            // used for COMPOSITE: page models (SIMPLE or COMPOSITE or MULTI)
        this.values = []            // used for MULTI: page models (SIMPLE or COMPOSITE)
        this.object = object        // the SIMPLE topic, the COMPOSITE topic, or the 1st MULTI topic ### FIXDOC
        this.object_type = object.get_type()
        this.value = object.value
        this.assoc_def = assoc_def
        this.uri = field_uri
        this.parent = parent_page_model
        this.label = page_model_label()
        this.input_field_rows = dm4c.get_view_config(this.object_type, "input_field_rows", assoc_def)
        var renderer_uri
        var renderer = lookup_renderer()
        var form_reading_function

        // === Simple Renderer ===

        this.render_info_single = function(parent_element, level) {
            renderer.render_info(this, parent_element, level)
        }

        this.render_form_single = function(parent_element, level) {
            form_reading_function = renderer.render_form(this, parent_element, level)
        }

        this.read_form_value = function() {
            if (!form_reading_function) {
                throw "PageModelError: the renderer for \"" + this.label + "\" provides no " +
                    "form reading function (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            var form_value = form_reading_function()
            // Note: undefined value is an error (means: simple renderer returned no value).
            // null is a valid result (means: simple renderer prevents the field from being updated).
            if (form_value === undefined) {
                throw "PageModelError: the form reading function for \"" + this.label +
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
                throw "PageModelError: the renderer for \"" + this.label + "\" provides no " +
                    "form reading function (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            var form_values = form_reading_function()
            // Note: undefined value is an error (means: simple renderer returned no value).
            // null is a valid result (means: simple renderer prevents the field from being updated).
            if (form_values === undefined) {
                throw "PageModelError: the form reading function for \"" + this.label +
                    "\" returned no value (renderer_uri=\"" + renderer_uri + "\")"
            }
            //
            return form_values
        }

        // ===

        function page_model_label() {
            var custom_assoc_type_uri = assoc_def && assoc_def.custom_assoc_type_uri
            if (custom_assoc_type_uri) {
                var custom_assoc_type = dm4c.get_association_type(custom_assoc_type_uri)
                if (custom_assoc_type.is_simple()) {
                    return custom_assoc_type.value
                }
            }
            return _self.object_type.value
        }

        function lookup_renderer() {
            switch (page_model_type) {
            case self.type.SIMPLE:
                renderer_uri = dm4c.get_view_config(_self.object_type, "simple_renderer_uri", assoc_def)
                return dm4c.get_simple_renderer(renderer_uri)
            case self.type.COMPOSITE:
                if (has_related_topic_page_model(assoc_def)) {
                    // Note: in case a related topic model underlies this page model it must always rendered
                    // by the default composite renderer, and not the object's one. The object may be simple.
                    renderer_uri = "dm4.webclient.default_composite_renderer"
                } else {
                    renderer_uri = dm4c.get_view_config(_self.object_type, "simple_renderer_uri", assoc_def)
                }
                return dm4c.get_simple_renderer(renderer_uri)
            case self.type.MULTI:
                renderer_uri = dm4c.get_view_config(_self.object_type, "multi_renderer_uri", assoc_def)
                return dm4c.get_multi_renderer(renderer_uri)
            default:
                throw "PageModelError: \"" + page_model_type + "\" is an unknown page model type"
            }
        }
    }

    function has_related_topic_page_model(assoc_def) {
        return assoc_def && assoc_def.custom_assoc_type_uri &&
            dm4c.get_association_type(assoc_def.custom_assoc_type_uri).is_composite()
    }

    /**
     * A "Related Topic Page Model" is a page model of type COMPOSITE which has exactly 2 childs:
     *     - "dm4.webclient.topic"
     *     - "dm4.webclient.relating_assoc"
     */
    function is_related_topic_page_model(page_model) {
        return page_model.childs["dm4.webclient.relating_assoc"]
    }

    // ------------------------------------------------------------------------------------------------------ Public API

    this.type = {
        SIMPLE: 1,
        COMPOSITE: 2,
        MULTI: 3
    }

    this.mode = {
        INFO: {
            render_setting: "hidden",
            render_func_name_single: "render_info_single",
            render_func_name_multi:  "render_info_multi"
        },
        FORM: {
            render_setting: "locked",
            render_func_name_single: "render_form_single",
            render_func_name_multi:  "render_form_multi"
        }
    }

    /**
     * Creates a page model for a topic or an association.
     * Depending on the topic's/associatios's type a SIMPLE or a COMPOSITE page model is created.
     *
     * A page model comprises all the information required to render a topic in the page panel (a.k.a. detail
     * panel). ### FIXDOC
     *
     * A page model is represented by a hierarchical structure of PageModel objects.
     *
     * @param   object              The object the page model is created for (a Topic or an Association).
     * @param   assoc_def           The association definition that leads to that topic. Undefined for the top-level
     *                              call. The association definition is used to create the PageModel object.
     *                              See there for further documentation. ### FIXDOC
     * @param   field_uri           The (base) URI for the (child) model(s) to create (string). Empty ("") for the
     *                              top-level call. The field URI is used to create the child PageModel objects.
     *                              See there for further documentation.
     * @param   render_mode         this.mode.INFO or this.mode.FORM (object).
     * @param   parent_page_model   The parent page model. Undefined for the top-level call. The parent page model
     *                              is stored in the "parent" property of the page model to be created. Renderers
     *                              are free to operate on it.
     *
     * @return  The created page model, or undefined.
     *          Undefined is returned if the object is a simple one and is hidden/locked.
     */
    this.create_page_model = function(object, assoc_def, field_uri, render_mode, parent_page_model) {
        if (has_related_topic_page_model(assoc_def)) {
            return create_related_topic_page_model()
        } else {
            return create_page_model(object, parent_page_model)
        }

        function create_related_topic_page_model() {
            var page_model = new PageModel(self.type.COMPOSITE, object, assoc_def, field_uri, parent_page_model)
            var relating_assoc = object.assoc && new Association(object.assoc) ||
                dm4c.empty_association(assoc_def.custom_assoc_type_uri)
            // Note: the Related Topic Page Model is set as the parent for its 2 childs. This is different than
            // with a MULTI page model (see next method). The 2 childs must be able to recognize their status
            // of being a Related Topic Page Model child (see is_removable() below).
            page_model.childs["dm4.webclient.relating_assoc"] = create_page_model(relating_assoc, page_model)
            page_model.childs["dm4.webclient.topic"]          = create_page_model(object, page_model)
            return page_model
        }

        function create_page_model(object, parent_page_model) {
            var object_type = object.get_type()
            if (object_type.is_simple()) {
                //
                if (dm4c.get_view_config(object_type, render_mode.render_setting, assoc_def)) {
                    return
                }
                //
                return new PageModel(self.type.SIMPLE, object, assoc_def, field_uri, parent_page_model)
            } else {
                var page_model = new PageModel(self.type.COMPOSITE, object, assoc_def, field_uri, parent_page_model)
                for (var i = 0, _assoc_def; _assoc_def = object_type.assoc_defs[i]; i++) {
                    self.extend_composite_page_model(object, _assoc_def, field_uri, render_mode, page_model)
                }
                return page_model;
            }
        }
    }

    /**
     * Creates a page model for a certain child of the given composite object, and adds it to the given page model.
     * The given association definition specifies which child to use.
     *
     * Note: the association definition is *not* required to be part of the object's type definition.
     * It may be part of a facet definition as well.
     *
     * This method is used in 2 situations:
     *   - called repeatedly to build up a composite object's page model (called internally from create_page_model).
     *   - called from the Facets plugin to extend an objects page model based on the object's facets.
     *
     * @param   object          A composite object.
     * @param   assoc_def       The association definition that leads to the object's child(s).
     * @param   page_model      The page model of the composite object as constructed so far.
     *                          This page model is extended by this method.
     */
    this.extend_composite_page_model = function(object, assoc_def, field_uri, render_mode, page_model) {
        var child_topic_type = dm4c.get_topic_type(assoc_def.child_type_uri)
        //
        if (dm4c.get_view_config(child_topic_type, render_mode.render_setting, assoc_def)) {
            return
        }
        //
        var child_field_uri = field_uri + dm4c.COMPOSITE_PATH_SEPARATOR + assoc_def.assoc_def_uri
        var cardinality_uri = assoc_def.child_cardinality_uri
        if (cardinality_uri == "dm4.core.one") {
            var child_topic = object.childs[assoc_def.assoc_def_uri] || dm4c.empty_topic(child_topic_type.uri)
            page_model.childs[assoc_def.assoc_def_uri] = this.create_page_model(child_topic, assoc_def,
                child_field_uri, render_mode, page_model)
        } else if (cardinality_uri == "dm4.core.many") {
            // ### TODO: server: don't send empty arrays
            var child_topics = object.childs[assoc_def.assoc_def_uri] || []
            if (!js.is_array(child_topics)) {
                throw "PageModelError: field \"" + assoc_def.assoc_def_uri + "\" is defined as multi-value " +
                    "but appears as single-value in " + JSON.stringify(object)
            }
            if (child_topics.length == 0) {
                child_topics.push(dm4c.empty_topic(child_topic_type.uri))
            }
            var multi_model = new PageModel(self.type.MULTI, child_topics[0], assoc_def, field_uri, page_model)
            for (var j = 0, child_topic; child_topic = child_topics[j]; j++) {
                // Note: the page models of a MULTI get the COMPOSITE as the parent page model, not the MULTI.
                // ### TODO: rethink about it. This is different than with a Related Topic Page Model (see
                // previous method).
                multi_model.values.push(this.create_page_model(child_topic, assoc_def, child_field_uri, render_mode,
                    page_model))
            }
            page_model.childs[assoc_def.assoc_def_uri] = multi_model
        } else {
            throw "PageModelError: \"" + cardinality_uri + "\" is an unexpected cardinality URI"
        }
    }

    /**
     * Renders a page model. Called recursively.
     *
     * @param   page_model      The page model to render (a PageModel object, type SIMPLE or COMPOSITE).
     * @param   parent_element  The element the rendering is appended to (a jQuery object).
     *                          Precondition: this element is already attached to the document.
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object).
     * @param   level           The nesting level (integer). Starts at 0.
     */
    this.render_page_model = function(page_model, parent_element, render_mode, level) {
        // Note: if the topic is hidden/locked the page model is undefined
        if (!page_model) {
            return
        }
        //
        if (page_model.type != this.type.SIMPLE && page_model.type != this.type.COMPOSITE) {
            throw "PageModelError: invalid page model"
        }
        //
        var box = this.render_box(page_model, parent_element, render_mode, level, is_removable())
        page_model[render_mode.render_func_name_single](box, level)

        // === "Remove" Button ===

        // Note: subject of removal is always a SIMPLE or a COMPOSITE, never a MULTI. So, the remove button
        // aspect is handled here at framework level. In contrast, the "Add" button is always bound to a MULTI.
        // So, the add button aspect is handled by the respective multi renderers.

        /**
         * Determines weather to render a remove button for the current page model.
         */
        function is_removable() {
            return render_mode == dm4c.render.page_model.mode.FORM &&
                page_model.assoc_def &&     // Note: the top-level page model has no assoc_def
                page_model.assoc_def.child_cardinality_uri == "dm4.core.many" &&
                !is_related_topic_page_model(page_model.parent)
                // Note: for childs of a Related Topic Page Model the remove button is already rendered there
        }
    }

    /**
     * Renders a box and attaches it to the DOM.
     *
     * @param   parent_element  The element the box is appended to (a jQuery object).
     *                          Precondition: this element is already attached to the document.
     */
    this.render_box = function(page_model, parent_element, render_mode, level, is_removable) {
        var box = $("<div>").addClass("box")
        var box_type = page_model.type          // SIMPLE, COMPOSITE, or MULTI
        var topic_id = page_model.object.id     // ID of the topic represented by the box. Undefined for MULTI.
        // Note: a SIMPLE box or a MULTI box doesn't get a "level" class to let it inherit the background color
        box.toggleClass("level" + level, box_type == this.type.COMPOSITE)
        // Note: only a SIMPLE and a COMPOSITE box represents a revealable child topic. A MULTI box does not.
        // Note: topic ID is -1 if there is no underlying topic in the DB. This is the case e.g. for a Search
        // topic's Search Result field.
        if (render_mode == this.mode.INFO && level == 1 && topic_id != -1 &&
                            (box_type == this.type.COMPOSITE || box_type == this.type.SIMPLE)) {
            box.addClass("topic").click(function() {
                dm4c.do_reveal_related_topic(topic_id, "show")
            })
        }
        // attach to DOM
        parent_element.append(box)
        //
        if (is_removable) {
            render_remove_button()
        }
        //
        return box

        /**
         * Renders a remove button and appends it to the box.
         * When the button is pressed the box is removed from the document.
         */
        function render_remove_button() {
            var remove_button = dm4c.ui.button({on_click: do_remove, icon: "circle-minus"})
            var remove_button_div = $("<div>").addClass("remove-button").append(remove_button)
            box.append(remove_button_div)

            function do_remove() {
                // update model
                page_model.object.delete = true
                // update view
                box.remove()
            }
        }
    }

    /**
     * Reads out values from a page model's GUI elements and builds an object model (a topic model or an
     * association model) from it.
     *
     * @return  a topic model (object), a topic reference (string), a simple topic value, or null. ### FIXDOC
     *          Note 1: null is returned if a simple topic is being edited and the simple renderer prevents the
     *          field from being updated.
     *          Note 2: despite at deeper recursion levels this method might return a topic reference (string), or
     *          a simple topic value, the top-level call will always return a topic model (object), or null.
     *          This is because topic references are only contained in composite topic models. A simple topic model
     *          on the other hand never represents a topic reference.
     */
    this.build_object_model = function(page_model) {
        if (page_model.type == self.type.SIMPLE) {
            var value = page_model.read_form_value()
            // Note: undefined form value is an error (means: simple renderer returned no value). Already thrown.
            // null is a valid form value (means: simple renderer prevents the field from being updated).
            if (value == null) {
                return null
            }
            var object_model = {
                type_uri: page_model.object.type_uri,
                // Note: the type URI is not strictly required for server-side processing, but for the
                // client-side "pre_update_topic"/"pre_update_association" listeners as they usually
                // examine the topic's/association's type.
                value: value
            }
            // Note: we never want *update* an aggregated simple child. Instead we either want *create* a new child
            // or *assign* an existing child. So we remove the ID from the object model. At server-side the update vs.
            // create decision is based on the existence of an ID.
            if ((!page_model.assoc_def || page_model.assoc_def.type_uri != "dm4.core.aggregation_def") &&
                                                                                    !is_del_ref(value)) {
                object_model.id = page_model.object.id
            }
            return object_model
        } else if (page_model.type == self.type.COMPOSITE) {
            if (is_related_topic_page_model(page_model)) {
                var topic_model = this.build_object_model(page_model.childs["dm4.webclient.topic"])
                // Note: if updating the topic field is prevented the relating assoc form input is ignored as well.
                if (topic_model != null) {
                    var assoc_model = this.build_object_model(page_model.childs["dm4.webclient.relating_assoc"])
                    // ### FIXME: check assoc_model for null?
                    topic_model.assoc = assoc_model
                }
                return topic_model
            } else {
                var object_model = page_model.read_form_value()
                return object_model
            }
        } else {
            throw "PageModelError: invalid page model"
        }

        // ### FIXME: must handle by-uri del refs as well?
        function is_del_ref(value) {
            // Note: value can be a boolean or number as well. js.begins_with() would fail.
            return typeof(value) == "string" && js.begins_with(value, dm4c.DEL_ID_PREFIX)
        }
    }
}
