/**
 * Provides the standard commands ("Create", "Edit", "Delete", "Hide", "Associate").
 */
dm4c.add_plugin("de.deepamehta.webclient.default", function() {

    // === Dialogs ===

    var delete_topic_dialog = dm4c.ui.dialog({
        title: "Delete Topic?",
        width: "300px",
        button_label: "Delete",
        button_handler: function() {
            delete_topic_dialog.close()
            dm4c.do_delete_topic(dm4c.selected_object)
            // ### FIXME: we should not operate on dm4c.selected_object. Theoretically it may have
            // changed since opening the dialog (practically it can't because the dialog is modal).
            // Instead we should set the button handler only when opening the dialog.
        }
    })
    var delete_association_dialog = dm4c.ui.dialog({
        title: "Delete Association?",
        width: "300px",
        button_label: "Delete",
        button_handler: function() {
            delete_association_dialog.close()
            dm4c.do_delete_association(dm4c.selected_object)
            // ### FIXME: we should not operate on dm4c.selected_object. Theoretically it may have
            // changed since opening the dialog (practically it can't because the dialog is modal).
            // Instead we should set the button handler only when opening the dialog.
        }
    })

    var type_menu = dm4c.ui.menu()
    var retype_topic_dialog = dm4c.ui.dialog({
        title: "Retype Topic",
        content: type_menu.dom,
        width: "400px",
        button_label: "Retype",
        button_handler: function() {
            var type_uri = type_menu.get_selection().value
            retype_topic_dialog.close()
            dm4c.do_retype_topic(dm4c.selected_object, type_uri)
            // ### FIXME: we should not operate on dm4c.selected_object. Theoretically it may have
            // changed since opening the dialog (practically it can't because the dialog is modal).
            // Instead we should set the button handler only when opening the dialog.
        }
    })

    // === Webclient Listeners ===

    dm4c.add_listener("topic_commands", function(topic) {
        var commands = []
        var is_writable = dm4c.has_write_permission_for_topic(topic)
        //
        commands.push({
            label: "Hide",
            handler: do_hide,
            context: "context-menu"
        })
        //
        if (is_writable) {
            if (!topic.get_type().is_locked()) {
                commands.push({is_separator: true, context: "context-menu"})
                commands.push({
                    label: "Edit",
                    handler: do_edit,
                    context: ["context-menu", "detail-panel-show"],
                    ui_icon: "pencil"
                })
            }
        }
        //
        commands.push({is_separator: true, context: "context-menu"})
        commands.push({
            label: "Associate",
            handler: do_associate,
            context: "context-menu"
        })
        //
        if (is_writable) {
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
                handler: do_confirm,
                context: "context-menu",
                ui_icon: "trash"
            })
        }
        //
        commands.push({
            label: "OK",
            handler: do_save,
            context: "detail-panel-edit",
            is_submit: true
        })
        //
        return commands

        // Note: all command handlers receive the coordinates of the command selecting mouse click,
        // however, most of them doesn't care. See function open_context_menu() in canvas.js

        function do_hide() {
            dm4c.do_hide_topic(topic)
        }

        function do_edit() {
            dm4c.enter_edit_mode(topic)
        }

        function do_associate(x, y) {
            dm4c.topicmap_renderer.begin_association(topic.id, x, y)
        }

        function do_retype() {
            dm4c.refresh_type_menu(type_menu)   // no filter_func specified
            type_menu.select(topic.type_uri)
            retype_topic_dialog.open()
        }

        function do_confirm() {
            delete_topic_dialog.open()
        }

        function do_save() {
            dm4c.page_panel.save()
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
        if (dm4c.has_write_permission_for_association(assoc)) {
            if (!assoc.get_type().is_locked()) {
                commands.push({is_separator: true, context: "context-menu"})
                commands.push({
                    label: "Edit",
                    handler: do_edit,
                    context: ["context-menu", "detail-panel-show"],
                    ui_icon: "pencil"
                })
            }
            //
            commands.push({is_separator: true, context: "context-menu"})
            commands.push({
                label: "Delete",
                handler: do_confirm,
                context: "context-menu",
                ui_icon: "trash"
            })
        }
        //
        commands.push({
            label: "OK",
            handler: do_save,
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

        function do_confirm() {
            delete_association_dialog.open()
        }

        function do_save() {
            dm4c.page_panel.save()
        }
    })

    dm4c.add_listener("canvas_commands", function(cx, cy) {
        var commands = []
        // Note: type_uri is undefined if the user has no create permission or has nothing created yet
        var type_uri = dm4c.toolbar.get_recent_type_uri()
        if (type_uri) {
            var type_label = dm4c.type_label(type_uri)
            commands.push({
                label: "Create " + type_label, handler: do_create, context: "context-menu"
            })
        }
        return commands

        function do_create() {
            dm4c.do_create_topic(type_uri, cx, cy)
        }
    })
})
