function workspaces_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.3-workspaces/style/workspaces.css")

    // ------------------------------------------------------------------------------------------------------ Public API



    // ************************************************************
    // *** Webclient Hooks (triggered by deepamehta3-webclient) ***
    // ************************************************************



    this.init = function() {

        var workspaces = get_all_workspaces()
        create_workspace_menu()
        create_workspace_dialog()

        function create_workspace_menu() {
            var workspace_label = $("<span>").attr("id", "workspace-label").text("Workspace")
            var workspace_menu = $("<div>").attr("id", "workspace-menu")
            var workspace_form = $("<div>").attr("id", "workspace-form").append(workspace_label).append(workspace_menu)
            $("#upper-toolbar").prepend(workspace_form)
            dm4c.ui.menu("workspace-menu", do_select_workspace)
            rebuild_workspace_menu(undefined, workspaces)
            update_cookie()
        }

        function create_workspace_dialog() {
            var workspace_dialog = $("<div>").attr("id", "workspace_dialog")
            var input = $("<input>").attr({id: "workspace_name", size: 30})
            workspace_dialog.append("Name:")
            workspace_dialog.append($("<form>").attr("action", "#").submit(do_create_workspace).append(input))
            $("body").append(workspace_dialog)
            $("#workspace_dialog").dialog({modal: true, autoOpen: false, draggable: false, resizable: false, width: 350,
                title: "New Workspace", buttons: {"OK": do_create_workspace}})
        }
    }

    /**
     * @param   topic   a Topic object
     */
    this.post_delete_topic = function(topic) {
        if (topic.type_uri == "dm4.workspaces.workspace") {
            rebuild_workspace_menu()
        }
    }



    // *********************************************************************
    // *** Access Control Hooks (triggered by deepamehta3-accesscontrol) ***
    // *********************************************************************



    this.user_logged_in = function(user) {
        rebuild_workspace_menu()
    }

    this.user_logged_out = function() {
        rebuild_workspace_menu()
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_all_workspaces() {
        return dm4c.restc.get_topics("dm4.workspaces.workspace", true)  // sort=true
    }

    /**
     * Reads out the workspace menu and returns the workspace ID.
     * If the workspace menu has no items yet, undefined is returned.
     */
    function get_workspace_id_from_menu() {
        var item = dm4c.ui.menu_item("workspace-menu")
        if (item) {
            return item.value
        }
    }

    function open_workspace_dialog() {
        $("#workspace_dialog").dialog("open")
    }

    function do_create_workspace() {
        $("#workspace_dialog").dialog("close")
        var name = $("#workspace_name").val()
        create_workspace(name)
        return false
    }

    /**
     * Invoked when the user made a selection from the workspace menu.
     */
    function do_select_workspace(menu_item) {
        var workspace_id = menu_item.value
        dm4c.log("Workspace selected: " + workspace_id)
        update_cookie()
        if (workspace_id == "_new") {
            open_workspace_dialog()
        } else {
            var workspace = dm4c.fetch_topic(workspace_id)
            dm4c.show_topic(workspace, "show")
        }
    }

    // ---

    /**
     * Creates a workspace with the given name and puts it in the workspace menu.
     */
    function create_workspace(name) {
        var workspace = create_workspace_topic(name)
        rebuild_workspace_menu(workspace.id)
    }

    /**
     * Creates a new workspace in the DB.
     */
    function create_workspace_topic(name) {
        return dm4c.create_topic("dm4.workspaces.workspace", {"dm4.workspaces.name": name})
    }

    // ---

    /**
     * @param   workspace_id    Optional: ID of the workspace to select.
     *                          If not given, the current selection is maintained.
     */
    function rebuild_workspace_menu(workspace_id, workspaces) {
        if (!workspace_id) {
            workspace_id = get_workspace_id_from_menu()
        }
        if (!workspaces) {
            workspaces = get_all_workspaces()
        }
        //
        dm4c.ui.empty_menu("workspace-menu")
        var icon_src = dm4c.get_icon_src("dm4.workspaces.workspace")
        // add workspaces to menu
        for (var i = 0, workspace; workspace = workspaces[i]; i++) {
            dm4c.ui.add_menu_item("workspace-menu", {label: workspace.value, value: workspace.id, icon: icon_src})
        }
        // add "New..." to menu
        if (dm4c.has_create_permission("dm4.workspaces.workspace")) {
            dm4c.ui.add_menu_separator("workspace-menu")
            dm4c.ui.add_menu_item("workspace-menu", {label: "New Workspace...", value: "_new", is_trigger: true})
        }
        //
        select_menu_item(workspace_id)
    }

    /**
     * Selects an item from the workspace menu.
     */
    function select_menu_item(workspace_id) {
        dm4c.ui.select_menu_item("workspace-menu", workspace_id)
        update_cookie()
    }

    /**
     * Sets a cookie that reflects the selected workspace.
     */
    function update_cookie() {
        js.set_cookie("dm4_workspace_id", get_workspace_id_from_menu())
    }
}
