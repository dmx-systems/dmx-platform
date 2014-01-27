/**
 * Provides the standard commands ("Create", "Edit", "Delete", "Hide", "Associate").
 */
dm4c.add_plugin("de.deepamehta.webclient.default", function() {

    // === Webclient Listeners ===

    dm4c.add_listener("topic_commands", function(topic) {
        var commands = []
        //
        commands.push({
            label: "Hide",
            handler: do_hide,
            context: "context-menu"
        })
        //
        if (is_topic_changable(topic)) {
            commands.push({is_separator: true, context: "context-menu"})
            commands.push({
                label: "Edit",
                handler: do_edit,
                context: ["context-menu", "detail-panel-show"],
                ui_icon: "pencil"
            })
        }
        // ### FIXME: check permission
        commands.push({is_separator: true, context: "context-menu"})
        commands.push({
            label: "Associate",
            handler: do_associate,
            context: "context-menu"
        })
        // ### FIXME: check locked state as well (at least for retype)
        if (dm4c.has_write_permission_for_topic(topic)) {
            commands.push({is_separator: true, context: "context-menu"})
            commands.push({
                label: "Retype",
                handler: do_retype,
                context: "context-menu",
                ui_icon: "transfer-e-w"
            })
            //
            commands.push({is_separator: true, context: "context-menu"})
            commands.push({
                label: "Delete",
                handler: topic.type_uri == "dm4.core.topic_type" ? open_delete_topic_type_dialog :
                         topic.type_uri == "dm4.core.assoc_type" ? open_delete_association_type_dialog :
                                                                   open_delete_topic_dialog,
                context: "context-menu",
                ui_icon: "trash"
            })
        }
        //
        commands.push({
            label: "OK",
            handler: dm4c.page_panel.save,
            context: "detail-panel-edit",
            is_submit: true
        })
        //
        return commands

        // Note: all command handlers receive the selected item and the coordinates of the selecting mouse click.
        // However, most of the handlers don't care. See BaseMenu's create_selection_handler() in gui_toolkit.js

        function do_hide() {
            dm4c.do_hide_topic(topic)
        }

        function do_edit() {
            dm4c.enter_edit_mode(topic)
        }

        function do_associate(item, x, y) {
            dm4c.topicmap_renderer.begin_association(topic.id, x, y)
        }

        function do_retype() {
            var type_menu = dm4c.ui.menu()
            dm4c.refresh_type_menu(type_menu)   // no type list specified
            type_menu.select(topic.type_uri)
            open_retype_topic_dialog()

            function open_retype_topic_dialog() {
                dm4c.ui.dialog({
                    title: "Retype Topic",
                    content: type_menu.dom,
                    width: "400px",
                    button_label: "Retype",
                    button_handler: function() {
                        var type_uri = type_menu.get_selection().value
                        dm4c.do_retype_topic(topic.id, type_uri)
                    }
                })
            }
        }

        // ---

        function open_delete_topic_dialog() {
            dm4c.ui.dialog({
                title: "Delete Topic?",
                width: "300px",
                button_label: "Delete",
                button_handler: function() {
                    dm4c.do_delete_topic(topic.id)
                }
            })
        }

        function open_delete_topic_type_dialog() {
            dm4c.ui.dialog({
                title: "Delete Topic Type?",
                width: "300px",
                button_label: "Delete",
                button_handler: function() {
                    dm4c.do_delete_topic_type(topic.uri)
                }
            })
        }

        function open_delete_association_type_dialog() {
            dm4c.ui.dialog({
                title: "Delete Association Type?",
                width: "300px",
                button_label: "Delete",
                button_handler: function() {
                    dm4c.do_delete_association_type(topic.uri)
                }
            })
        }
    })

    dm4c.add_listener("association_commands", function(assoc) {
        var commands = []
        //
        commands.push({
            label: "Hide",
            handler: do_hide,
            context: "context-menu"
        })
        //
        if (is_association_changable(assoc)) {
            commands.push({is_separator: true, context: "context-menu"})
            commands.push({
                label: "Edit",
                handler: do_edit,
                context: ["context-menu", "detail-panel-show"],
                ui_icon: "pencil"
            })
        }
        //
        if (dm4c.has_write_permission_for_association(assoc)) {
            commands.push({is_separator: true, context: "context-menu"})
            commands.push({
                label: "Delete",
                handler: open_delete_association_dialog,
                context: "context-menu",
                ui_icon: "trash"
            })
        }
        //
        commands.push({
            label: "OK",
            handler: dm4c.page_panel.save,
            context: "detail-panel-edit",
            is_submit: true
        })
        //
        return commands

        function do_hide() {
            dm4c.do_hide_association(assoc)
        }

        function do_edit() {
            dm4c.enter_edit_mode(assoc)
        }

        function open_delete_association_dialog() {
            dm4c.ui.dialog({
                title: "Delete Association?",
                width: "300px",
                button_label: "Delete",
                button_handler: function() {
                    dm4c.do_delete_association(assoc.id)
                }
            })
        }
    })

    dm4c.add_listener("canvas_commands", function(cx, cy) {
        var commands = []
        var topic_types = dm4c.topic_type_list()
        if (topic_types.length) {
            commands.push({
                label: "Create",
                disabled: true,
                context: "context-menu"
            })
            for (var i = 0, topic_type; topic_type = topic_types[i]; i++) {
                commands.push({
                    label: topic_type.value,
                    icon:  topic_type.get_icon_src(),
                    handler: create_handler(topic_type.uri),
                    context: "context-menu"
                })
            }
        }
        return commands

        function create_handler(type_uri) {
            return function() {
                dm4c.do_create_topic(type_uri, cx, cy)
            }
        }
    })

    dm4c.add_listener("topic_doubleclicked", function(topic) {
        if (is_topic_changable(topic)) {
            dm4c.enter_edit_mode(topic)
        }
    })

    dm4c.add_listener("association_doubleclicked", function(assoc) {
        if (is_association_changable(assoc)) {
            dm4c.enter_edit_mode(assoc)
        }
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    function is_topic_changable(topic) {
        return dm4c.has_write_permission_for_topic(topic) && !topic.get_type().is_locked()
    }

    function is_association_changable(assoc) {
        return dm4c.has_write_permission_for_association(assoc) && !assoc.get_type().is_locked()
    }
})
