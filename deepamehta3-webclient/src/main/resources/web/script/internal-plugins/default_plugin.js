function default_plugin () {

    // ------------------------------------------------------------------------------------------------------ Public API



    // ************************************************************
    // *** Webclient Hooks (triggered by deepamehta3-webclient) ***
    // ************************************************************



    this.init = function() {

        dm3c.ui.dialog("delete-topic-dialog",       "Delete Topic?",       "Delete", do_delete_topic)
        dm3c.ui.dialog("delete-association-dialog", "Delete Association?", "Delete", do_delete_association)

        function do_delete_topic() {
            $("#delete-topic-dialog").dialog("close")
            dm3c.delete_topic(dm3c.selected_object)
        }

        function do_delete_association() {
            $("#delete-association-dialog").dialog("close")
            // update model
            dm3c.delete_association(dm3c.selected_object.id)
            // update view
            dm3c.canvas.refresh()
            dm3c.page_panel.clear()
        }
    }

    this.add_topic_commands = function(topic) {
        var commands = []
        //
        commands.push({label: "Hide",       handler: do_hide,      context: "context-menu"})
        commands.push({is_separator: true,                         context: "context-menu"})
        commands.push({label: "Associate",  handler: do_associate, context: "context-menu"})
        //
        if (dm3c.has_write_permission(topic)) {
            commands.push({label: "Edit",   handler: do_edit,      context: "detail-panel-show", ui_icon: "pencil"})
            commands.push({label: "Delete", handler: do_confirm,   context: "detail-panel-show", ui_icon: "trash"})
        }
        //
        commands.push({label: "Save",   handler: do_save,   context: "detail-panel-edit", ui_icon: "circle-check",
                                                                                          is_submit: true})
        commands.push({label: "Cancel", handler: do_cancel, context: "detail-panel-edit"})
        //
        return commands

        function do_hide() {
            dm3c.hide_topic(topic.id)
        }

        function do_associate(event) {
            dm3c.canvas.begin_association(topic.id, event)
        }

        function do_edit() {
            dm3c.begin_editing(topic)
        }

        function do_confirm() {
            $("#delete-topic-dialog").dialog("open")
        }

        function do_save() {
            dm3c.trigger_page_renderer_hook(topic, "process_form", topic)
        }

        function do_cancel() {
            dm3c.trigger_plugin_hook("post_submit_form", topic)
            dm3c.page_panel.refresh()
        }
    }

    this.add_association_commands = function(assoc) {
        var commands = []
        //
        commands.push({label: "Hide",       handler: do_hide,      context: "context-menu"})
        commands.push({is_separator: true,                         context: "context-menu"})
        commands.push({label: "Associate",  handler: do_associate, context: "context-menu"})
        //
        if (dm3c.has_write_permission(assoc)) {
            commands.push({label: "Edit",   handler: do_edit,      context: "detail-panel-show", ui_icon: "pencil"})
            commands.push({label: "Delete", handler: do_confirm,   context: "detail-panel-show", ui_icon: "trash"})
        }
        //
        commands.push({label: "Save",   handler: do_save,   context: "detail-panel-edit", ui_icon: "circle-check",
                                                                                          is_submit: true})
        commands.push({label: "Cancel", handler: do_cancel, context: "detail-panel-edit"})
        //
        return commands

        function do_hide() {
            // update model
            dm3c.hide_association(assoc.id)
            // update view
            dm3c.canvas.refresh()
            dm3c.page_panel.clear()
        }

        function do_associate(event) {
            dm3c.canvas.begin_association(assoc.id, event)  // TODO
        }

        function do_edit() {
            dm3c.begin_editing(assoc)
        }

        function do_confirm() {
            $("#delete-association-dialog").dialog("open")
        }

        function do_save() {
            dm3c.trigger_page_renderer_hook(assoc, "process_form", assoc)
        }

        function do_cancel() {
            dm3c.trigger_plugin_hook("post_submit_form", assoc)
            dm3c.page_panel.refresh()
        }
    }

    this.add_canvas_commands = function(cx, cy) {
        var commands = []
        var type_item = dm3c.ui.menu_item("create-type-menu")
        // Note: if the user has no create permission the type menu is empty (no items).
        if (type_item) {
            var type_uri = type_item.value
            var type_label = dm3c.type_label(type_uri)
            commands.push({
                label: "Create " + type_label, handler: do_create, context: "context-menu"
            })
        }
        return commands

        function do_create() {
            dm3c.create_topic_from_menu(type_uri, cx, cy)
        }
    }
}
