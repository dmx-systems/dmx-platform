dm4c.add_plugin("de.deepamehta.typeeditor", function() {

    // Note: no "uri" is set here. A new topic type gets its default URI at server-side.
    // Note: also the "type_uri" is provided at server-side (see TopicTypeModel constructor).
    var DEFAULT_TOPIC_TYPE = {
        value: "Topic Type Name",
        data_type_uri: "dm4.core.text",
        index_mode_uris: ["dm4.core.fulltext"],
        view_config_topics: [
            {
                type_uri: "dm4.webclient.view_config",
                childs: {
                    "dm4.webclient.show_in_create_menu": true
                }
            }
        ]
    }

    // Note: no "uri" is set here. A new association type gets its default URI at server-side.
    // Note: also the "type_uri" is provided at server-side (see AssociationTypeModel constructor).
    var DEFAULT_ASSOC_TYPE = {
        value: "Association Type Name",
        data_type_uri: "dm4.core.text",
        index_mode_uris: ["dm4.core.fulltext"],
        view_config_topics: [
            {
                type_uri: "dm4.webclient.view_config",
                childs: {
                    "dm4.webclient.show_in_create_menu": true    // ### TODO
                }
            }
        ]
    }

    // Note: no "uri" is set here. A new role type gets its default URI at server-side.
    // Note: also the "type_uri" is provided at server-side (see CoreServiceImpl#createRoleType()).
    var DEFAULT_ROLE_TYPE = {
        value: "Role Type Name"
    }

    // ---

    function show_type_warning(type_name) {
        alert("WARNING: Custom Association Type \"" + type_name + "\" is invalid\n\n" +
            "You can only choose among the existing association types. If you want create " +
            "another association type use the Create menu first.")
    }



    // === Webclient Listeners ===

    /**
     * Once a topic type is created we refresh the "Create" type menu.
     *
     * @param   topic   The topic just created.
     *                  Note: in case the just created topic is a type, the entire type definition is passed.
     */
    dm4c.add_listener("post_create_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            dm4c.refresh_create_menu()
        }
    })

    /**
     * Once a topic type is updated we refresh the "Create" type menu.
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            dm4c.refresh_create_menu()
        }
    })

    dm4c.add_listener("pre_submit_form", function(object, new_model) {
        if (new_model.type_uri == "dm4.core.composition_def" || new_model.type_uri == "dm4.core.aggregation_def") {
            // Note: when retyping a non-composite assoc new_model contains no childs
            var child = new_model.childs && new_model.childs["dm4.core.assoc_type#dm4.core.custom_assoc_type"]
            // Note: when nothing is entered and nothing was entered before new_model doesn't contain that child
            if (child) {
                var val = child.value
                if (val && !js.begins_with(val, dm4c.REF_ID_PREFIX) && !js.begins_with(val, dm4c.DEL_ID_PREFIX)) {
                    show_type_warning(val)
                    delete new_model.childs["dm4.core.assoc_type#dm4.core.custom_assoc_type"]
                }
            }
        }
    })

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        // Note: the toolbar's Create menu is only refreshed when the login status changes, not when a workspace is
        // selected. (At workspace selection time the Create menu is not refreshed but shown/hidden in its entirety.)
        // So, we check the READ permission here, not the CREATE permission. (The CREATE permission involves the
        // WRITEability of the selected workspace.)
        var tt = dm4c.has_read_permission_for_topic_type("dm4.core.topic_type")
        var at = dm4c.has_read_permission_for_topic_type("dm4.core.assoc_type")
        var rt = dm4c.has_read_permission_for_topic_type("dm4.core.role_type")
        //
        if (tt || at || rt) {
            type_menu.add_separator()
        }
        //
        if (tt) {
            type_menu.add_item({label: "New Topic Type", handler: function() {
                dm4c.do_create_topic_type(DEFAULT_TOPIC_TYPE)
            }})
        }
        if (at) {
            type_menu.add_item({label: "New Association Type", handler: function() {
                dm4c.do_create_association_type(DEFAULT_ASSOC_TYPE)
            }})
        }
        if (rt) {
            type_menu.add_item({label: "New Role Type", handler: function() {
                dm4c.do_create_role_type(DEFAULT_ROLE_TYPE)
            }})
        }
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    this.show_type_warning = function(type_name) {
        show_type_warning(type_name)
    }
})
