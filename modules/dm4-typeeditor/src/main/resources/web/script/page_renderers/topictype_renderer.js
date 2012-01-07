function TopictypeRenderer() {

    var value_input     // a jQuery <input> element
    var uri_input       // a jQuery <input> element
    var data_type_menu  // a GUIToolkit Menu object
    var editors_list    // a jQuery <ul> element

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {
        var topic_type = dm4c.get_topic_type(topic.uri)
        //
        dm4c.render.field_label("Name")
        dm4c.render.field_value(topic_type.value)
        //
        dm4c.render.field_label("URI")
        dm4c.render.field_value(topic_type.uri)
        //
        var data_type = dm4c.restc.get_topic_by_value("uri", topic_type.data_type_uri)
        dm4c.render.field_label("Data Type")
        dm4c.render.field_value(data_type.value)
        //
        dm4c.render.associations(topic.id)
    }

    this.render_form = function(topic) {
        var topic_type = dm4c.get_topic_type(topic.uri)
        //
        value_input = dm4c.render.input(topic_type.value)
        dm4c.render.field_label("Name")
        dm4c.render.field_value(value_input)
        //
        uri_input = dm4c.render.input(topic_type.uri)
        dm4c.render.field_label("URI")
        dm4c.render.field_value(uri_input)
        //
        data_type_menu = dm4c.render.topic_menu("dm4.core.data_type", topic_type.data_type_uri)
        dm4c.render.field_label("Data Type")
        dm4c.render.field_value(data_type_menu.dom)
        //
        if (topic_type.data_type_uri == "dm4.core.composite") {
            dm4c.render.field_label("Composed Topic Types (" + topic_type.assoc_defs.length + ")")
            render_assoc_def_editors()
        }

        function render_assoc_def_editors() {
            editors_list = $("<ul>").attr("id", "assoc-def-editors")
            dm4c.render.page(editors_list)
            for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
                var label_state = topic_type.get_label_config(assoc_def.uri)
                editors_list.append(new AssociationDefEditor(assoc_def, label_state).dom)
            }
            editors_list.sortable()
        }

        /**
         * @param   label_state     a boolean
         */
        function AssociationDefEditor(assoc_def, label_state) {
            var whole_type_label = $("<span>").addClass("label").text(topic_type.value)
            var part_type_label = $("<span>").addClass("label").text(dm4c.type_label(assoc_def.part_topic_type_uri))
            var whole_card_menu = dm4c.render.topic_menu("dm4.core.cardinality", assoc_def.whole_cardinality_uri)
            var part_card_menu = dm4c.render.topic_menu("dm4.core.cardinality", assoc_def.part_cardinality_uri)
            var assoc_type_label = $("<span>").addClass("label").addClass("field-label").text("Association Type")
            var assoc_type_menu = create_assoc_type_menu(assoc_def.assoc_type_uri)
            var label_config_checkbox = dm4c.render.checkbox(label_state)
            var label_config_label = $("<span>").addClass("label").addClass("field-label").text("Include in Label")
            //
            var optional_card_div = $("<div>").append(whole_type_label).append(whole_card_menu.dom)
            optional_card_div.toggle(is_aggregation_selected())
            //
            this.dom = $("<li>").addClass("assoc-def-editor").addClass("ui-state-default")
                .append($("<div>").append(part_type_label).append(part_card_menu.dom)
                                  .append(label_config_checkbox).append(label_config_label))
                .append(optional_card_div)
                .append($("<div>").append(assoc_type_label).append(assoc_type_menu.dom))
                .data("model_func", get_model)

            function create_assoc_type_menu(selected_uri) {
                var menu = dm4c.ui.menu(do_refresh_opional_card_div)
                menu.add_item({label: "Composition Definition", value: "dm4.core.composition_def"})
                menu.add_item({label: "Aggregation Definition", value: "dm4.core.aggregation_def"})
                menu.select(selected_uri)
                return menu

                function do_refresh_opional_card_div() {
                    if (is_aggregation_selected()) {
                        optional_card_div.show(500)
                    } else {
                        optional_card_div.hide(500)
                    }
                }
            }

            function is_aggregation_selected() {
                return assoc_type_menu.get_selection().value == "dm4.core.aggregation_def"
            }

            function get_model() {
                return {
                    assoc_def: {
                        id:                    assoc_def.id,
                        part_topic_type_uri:   assoc_def.part_topic_type_uri,
                        part_cardinality_uri:  part_card_menu.get_selection().value,
                        whole_cardinality_uri: whole_card_menu.get_selection().value,
                        assoc_type_uri:        assoc_type_menu.get_selection().value
                    },
                    label_state: label_config_checkbox.get(0).checked
                }
            }
        }
    }

    this.process_form = function(topic) {
        var topic_type_model = build_topic_type_model()
        dm4c.do_update_topic_type(topic, topic_type_model)
        dm4c.trigger_plugin_hook("post_submit_form", topic)

        /**
         * Reads out values from GUI elements and builds a topic type model object from it.
         *
         * @return  a topic type model object
         */
        function build_topic_type_model() {
            var topic_type_model = {
                id: topic.id,
                uri: $.trim(uri_input.val()),
                value: $.trim(value_input.val()),
                data_type_uri: data_type_menu.get_selection().value
            }
            //
            var topic_type = dm4c.get_topic_type(topic.uri)
            if (topic_type.data_type_uri == "dm4.core.composite") {
                var model = composite_model()
                topic_type_model.assoc_defs   = model.assoc_defs
                topic_type_model.label_config = model.label_config
            }
            //
            return topic_type_model

            function composite_model() {
                var assoc_defs = []
                var label_config = []
                editors_list.children().each(function() {
                    var editor_model = $(this).data("model_func")()
                    var assoc_def = editor_model.assoc_def
                    assoc_defs.push(assoc_def)
                    if (editor_model.label_state) {
                        label_config.push(assoc_def.part_topic_type_uri)
                    }
                })
                return {
                    assoc_defs: assoc_defs,
                    label_config: label_config
                }
            }
        }
    }
}
