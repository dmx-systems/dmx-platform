function WorkspacesPluginView(controller) {

    // ------------------------------------------------------------------------------------------------------- Constants

    var WORKSPACE_INFO_BUTTON_HELP = "Reveal the selected workspace on the topicmap.\n\n" +
        "Use this to rename/delete the workspace or to inspect its settings."

    var SHARING_MODE_HELP = {
        "dm4.workspaces.private":
            "Only you get access to the workspace content.\n\n" +
            "Use this for your privacy.",
        "dm4.workspaces.confidential":
            "Workspace members get READ access to the workspace content.\n" +
            "Only you get WRITE access to the workspace content.\n" +
            "Only you can create memberships.\n\n" +
            "Use this if you want make content available to a closed user group you control (like a mailing).",
        "dm4.workspaces.collaborative":
            "Workspace members get READ/WRITE access to the workspace content.\n" +
            "Each workspace member can create further memberships.\n\n" +
            "Use this if you want work together with a user group (like a groupware). Each user get equal rights.",
        "dm4.workspaces.public":
            "Every user of this DeepaMehta installation (logged in or not) get READ access to the workspace " +
                "content.\n" +
            "Workspace members get READ/WRITE access to the workspace content.\n" +
            "Each workspace member can create further memberships.\n\n" +
            "Use this if you want make content available to the public (like a blog).",
        "dm4.workspaces.common":
            "Every user of this DeepaMehta installation (logged in or not) get READ/WRITE access to the workspace " +
                "content.\n\n" +
            "Use this for content belonging to the commons (like a wiki)."
    }

    // --------------------------------------------------------------------------------------------------- Private State

    var workspace_menu          // A GUIToolkit Menu object

    // ------------------------------------------------------------------------------------------------------ Public API

    this.create_workspace_widget = create_workspace_widget
    this.refresh_workspace_menu = refresh_workspace_menu
    this.refresh_menu_item = refresh_menu_item
    this.open_assign_workspace_dialog = open_assign_workspace_dialog

    // ----------------------------------------------------------------------------------------------- Private Functions



    // === Workspace Widget ===

    function create_workspace_widget() {
        var workspace_label = $("<span>").attr("id", "workspace-label").text("Workspace")
        workspace_menu = dm4c.ui.menu(do_select_workspace)
        var workspace_info_button = dm4c.ui.button({on_click: do_reveal_workspace, icon: "info"})
            .attr({title: WORKSPACE_INFO_BUTTON_HELP})
        var workspace_widget = $("<div>").attr("id", "workspace-widget")
            .append(workspace_label)
            .append(workspace_menu.dom)
            .append(workspace_info_button)
        // put in toolbar
        dm4c.toolbar.dom.prepend(workspace_widget)
        //
        refresh_workspace_menu()

        function do_select_workspace(menu_item) {
            var workspace_id = menu_item.value
            if (workspace_id == "_new") {
                open_new_workspace_dialog()
            } else {
                controller.select_workspace(workspace_id)
            }
        }

        function do_reveal_workspace() {
            dm4c.do_reveal_topic(controller.get_selected_workspace_id(), "show")
        }
    }

    /**
     * Refreshes the workspace menu based on the model ("workspaces", "selected_workspace_id").
     */
    function refresh_workspace_menu() {
        workspace_menu.empty()
        add_workspaces_to_menu(workspace_menu)
        //
        if (is_logged_in()) {
            workspace_menu.add_separator()
            workspace_menu.add_item({label: "New Workspace...", value: "_new", is_trigger: true})
        }
        //
        refresh_menu_item()
    }

    /**
     * Selects an item from the workspace menu based on the model ("selected_workspace_id").
     */
    function refresh_menu_item() {
        workspace_menu.select(controller.get_selected_workspace_id())
    }



    // === Workspace Dialogs ===

    function open_new_workspace_dialog() {
        var name_input = dm4c.render.input(undefined, 30)
        var sharing_mode_selector = sharing_mode_selector()
        dm4c.ui.dialog({
            title: "New Workspace",
            content: dm4c.render.label("Name").add(name_input)
                .add(dm4c.render.label("Sharing Mode")).add(sharing_mode_selector),
            button_label: "Create",
            button_handler: do_create_workspace
        })

        function sharing_mode_selector() {
            var enabled_sharing_modes = get_enabled_sharing_modes()
            var _checked
            var selector = $()
            add_sharing_mode("Private",       "dm4.workspaces.private")
            add_sharing_mode("Confidential",  "dm4.workspaces.confidential")
            add_sharing_mode("Collaborative", "dm4.workspaces.collaborative")
            add_sharing_mode("Public",        "dm4.workspaces.public")
            add_sharing_mode("Common",        "dm4.workspaces.common")
            return selector

            function get_enabled_sharing_modes() {
                var username_topic        = dm4c.get_plugin("de.deepamehta.accesscontrol").get_username_topic()
                var enabled_sharing_modes = dm4c.get_plugin("de.deepamehta.config").get_config_topic(username_topic.id,
                    "dm4.workspaces.enabled_sharing_modes")
                return enabled_sharing_modes
            }

            function add_sharing_mode(name, sharing_mode_uri) {
                var enabled = is_sharing_mode_enabled(sharing_mode_uri)
                var checked = get_checked(enabled)
                selector = selector
                    .add($("<label>").attr("title", SHARING_MODE_HELP[sharing_mode_uri])
                        .append($("<input>").attr({
                            type: "radio", name: "sharing-mode", value: sharing_mode_uri, disabled: !enabled,
                            checked: checked
                        }))
                        .append($("<span>").toggleClass("ui-state-disabled", !enabled).text(name))
                    )
                    .add($("<br>"))
            }

            function is_sharing_mode_enabled(sharing_mode_uri) {
                return enabled_sharing_modes.childs[sharing_mode_uri + ".enabled"].value
            }

            function get_checked(enabled) {
                if (enabled && !_checked) {
                    _checked = true
                    return true
                }
            }
        }

        function do_create_workspace() {
            var name = name_input.val()
            var sharing_mode_uri = sharing_mode_selector.find(":checked").val()
            controller.create_workspace(name, sharing_mode_uri)
        }
    }

    function open_assign_workspace_dialog(object_id, object_info) {
        var workspace_menu = workspace_menu()
        dm4c.ui.dialog({
            title: "Assign " + object_info + " to Workspace",
            content: workspace_menu.dom,
            width: "300px",
            button_label: "Assign",
            button_handler: do_assign_to_workspace
        })

        function workspace_menu() {
            var workspace_menu = dm4c.ui.menu()
            var workspace = dm4c.restc.get_assigned_workspace(object_id)
            var workspace_id = workspace && workspace.id
            add_workspaces_to_menu(workspace_menu, function(workspace) {
                return dm4c.has_write_permission_for_topic(workspace.id)
            })
            workspace_menu.select(workspace_id)
            return workspace_menu
        }

        function do_assign_to_workspace() {
            var workspace_id = workspace_menu.get_selection().value
            dm4c.restc.assign_to_workspace(object_id, workspace_id)
            dm4c.page_panel.refresh()
        }
    }



    // === Utilities ===

    function add_workspaces_to_menu(menu, filter_func) {
        var icon_src = dm4c.get_type_icon_src("dm4.workspaces.workspace")
        var workspaces = controller.get_workspaces()
        for (var i = 0, workspace; workspace = workspaces[i]; i++) {
            if (!filter_func || filter_func(workspace)) {
                menu.add_item({label: workspace.value, value: workspace.id, icon: icon_src})
            }
        }
    }

    function is_logged_in() {
        return dm4c.get_plugin("de.deepamehta.accesscontrol").get_username()
    }
}
// Enable debugging for dynamically loaded scripts:
//# sourceURL=workspaces_plugin_view.js
