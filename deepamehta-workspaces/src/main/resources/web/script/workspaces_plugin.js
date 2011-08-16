function workspaces_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.workspaces/style/workspaces.css")

    // view
    var workspace_menu

    // ------------------------------------------------------------------------------------------------------ Public API



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {

        var workspaces = get_all_workspaces()
        create_workspace_menu()
        create_workspace_dialog()

        function create_workspace_menu() {
            // build workspace widget
            var workspace_label = $("<span>").attr("id", "workspace-label").text("Workspace")
            workspace_menu = dm4c.ui.menu(do_select_workspace)
            var workspace_form = $("<div>").attr("id", "workspace-form")
                .append(workspace_label)
                .append(workspace_menu.dom)
            // put in toolbar
            dm4c.toolbar.dom.prepend(workspace_form)
            //
            rebuild_workspace_menu(undefined, workspaces)
            update_cookie()
        }

        function create_workspace_dialog() {
            var workspace_dialog = $("<div>")
                .append($("<div>").addClass("field-label").text("Name"))
                .append($("<form>").attr("action", "#").submit(do_create_workspace)
                    .append($("<input>").attr({id: "workspace_name", size: 30})))
            dm4c.ui.dialog("workspace-dialog", "New Workspace", workspace_dialog, "auto", "OK", do_create_workspace)
        }
    }

    /**
     * @param   topic   a Topic object
     */
    this.post_update_topic = function(topic, old_topic) {
        if (topic.type_uri == "dm4.workspaces.workspace") {
            rebuild_workspace_menu()
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



    // ********************************************************************
    // *** Access Control Hooks (triggered by deepamehta-accesscontrol) ***
    // ********************************************************************



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
        var item = workspace_menu.get_selection()
        if (item) {
            return item.value
        }
    }

    function open_workspace_dialog() {
        $("#workspace-dialog").dialog("open")
    }

    function do_create_workspace() {
        $("#workspace-dialog").dialog("close")
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
     *                          If not given, the current selection is preserved.
     */
    function rebuild_workspace_menu(workspace_id, workspaces) {
        if (!workspace_id) {
            workspace_id = get_workspace_id_from_menu()
        }
        if (!workspaces) {
            workspaces = get_all_workspaces()
        }
        //
        workspace_menu.empty()
        var icon_src = dm4c.get_icon_src("dm4.workspaces.workspace")
        // add workspaces to menu
        for (var i = 0, workspace; workspace = workspaces[i]; i++) {
            workspace_menu.add_item({label: workspace.value, value: workspace.id, icon: icon_src})
        }
        // add "New..." to menu
        if (dm4c.has_create_permission("dm4.workspaces.workspace")) {
            workspace_menu.add_separator()
            workspace_menu.add_item({label: "New Workspace...", value: "_new", is_trigger: true})
        }
        //
        select_menu_item(workspace_id)
    }

    /**
     * Selects an item from the workspace menu.
     */
    function select_menu_item(workspace_id) {
        workspace_menu.select(workspace_id)
        update_cookie()
    }

    /**
     * Sets a cookie that reflects the selected workspace.
     */
    function update_cookie() {
        js.set_cookie("dm4_workspace_id", get_workspace_id_from_menu())
    }
}
