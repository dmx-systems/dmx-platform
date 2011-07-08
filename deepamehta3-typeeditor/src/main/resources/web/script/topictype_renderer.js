function TopictypeRenderer() {

    var value_input     // a jQuery <input> element
    var uri_input       // a jQuery <input> element
    var data_type_menu  // a UIHelper Menu object

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {
        var topic_type = dm3c.type_cache.get_topic_type(topic.uri)
        //
        dm3c.render.field_label("Topic Type")
        dm3c.render.field_value(topic_type.value)
        //
        dm3c.render.field_label("URI")
        dm3c.render.field_value(topic_type.uri)
        //
        var data_type = dm3c.restc.get_topic_by_value("uri", topic_type.data_type_uri)
        dm3c.render.field_label("Data Type")
        dm3c.render.field_value(data_type.value)
        //
        dm3c.render.associations(topic.id)
    }

    this.render_form = function(topic) {
        var topic_type = dm3c.type_cache.get_topic_type(topic.uri)
        //
        value_input = dm3c.render.input(topic_type.value)
        dm3c.render.field_label("Topic Type")
        dm3c.render.field_value(value_input)
        //
        uri_input = dm3c.render.input(topic_type.uri)
        dm3c.render.field_label("URI")
        dm3c.render.field_value(uri_input)
        //
        data_type_menu = dm3c.render.topic_menu("dm3.core.data_type", topic_type.data_type_uri)
        dm3c.render.field_label("Data Type")
        dm3c.render.field_value(data_type_menu.dom)
        //
        if (topic_type.data_type_uri == "dm3.core.composite") {
            dm3c.render.field_label("Composed Topic Types (" + topic_type.assoc_defs.length + ")")
            render_assoc_def_editors()
        }

        function render_assoc_def_editors() {
            var editors_list = $("<ul>").attr("id", "assoc-def-editors")
            dm3c.render.page(editors_list)
            for (var i = 0, assoc_def; assoc_def = topic_type.assoc_defs[i]; i++) {
                editors_list.append(new AssociationDefEditor(assoc_def).dom)
            }
            $("#assoc-def-editors").sortable()
        }

        function AssociationDefEditor(assoc_def) {
            var whole_type_label = $("<span>").addClass("label").text(topic_type.value)
            var part_type_label = $("<span>").addClass("label").text(dm3c.type_label(assoc_def.part_topic_type_uri))
            var whole_card_menu = dm3c.render.topic_menu("dm3.core.cardinality", assoc_def.whole_cardinality_uri)
            var part_card_menu = dm3c.render.topic_menu("dm3.core.cardinality", assoc_def.part_cardinality_uri)
            var assoc_type_label = $("<span>").addClass("label").addClass("field-label").text("Association Type")
            var assoc_type_menu = create_assoc_type_menu(assoc_def.assoc_type_uri)
            //
            var optional_card_div = $("<div>").append(whole_type_label).append(whole_card_menu.dom)
            optional_card_div.toggle(is_aggregation_selected())
            //
            this.dom = $("<li>").addClass("assoc-def-editor").addClass("ui-state-default")
                .append($("<div>").append(part_type_label).append(part_card_menu.dom))
                .append(optional_card_div)
                .append($("<div>").append(assoc_type_label).append(assoc_type_menu.dom))

            function create_assoc_type_menu(selected_uri) {
                var menu = dm3c.ui.menu(assoc_def.part_topic_type_uri, refresh_opional_card_div)
                menu.add_item({label: "Composition Definition", value: "dm3.core.composition_def"})
                menu.add_item({label: "Aggregation Definition", value: "dm3.core.aggregation_def"})
                menu.select(selected_uri)
                return menu
            }

            function refresh_opional_card_div() {
                if (is_aggregation_selected()) {
                    optional_card_div.show(500)
                } else {
                    optional_card_div.hide(500)
                }
            }

            function is_aggregation_selected() {
                return assoc_type_menu.get_selection().value == "dm3.core.aggregation_def"
            }
        }
    }

    this.process_form = function(topic) {
        var topic_type_model = build_topic_type_model()
        var topic_type = dm3c.do_update_topic_type(topic, topic_type_model)
        dm3c.trigger_plugin_hook("post_submit_form", topic)

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
            return topic_type_model
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

}
