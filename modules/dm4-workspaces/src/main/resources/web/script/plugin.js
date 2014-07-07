dm4c.add_plugin("de.deepamehta.workspaces", function() {

    // View
    var workspace_menu      // A GUIToolkit Menu object



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {

        var workspaces = get_all_workspaces()

        create_workspace_menu()

        function create_workspace_menu() {
            // build workspace widget
            var workspace_label = $("<span>").attr("id", "workspace-label").text("Workspace")
            workspace_menu = dm4c.ui.menu(do_select_workspace)
            var workspace_form = $("<div>").attr("id", "workspace-widget")
                .append(workspace_label)
                .append(workspace_menu.dom)
            // put in toolbar
            dm4c.toolbar.dom.prepend(workspace_form)
            //
            refresh_workspace_menu(undefined, workspaces)
            update_cookie()

            function do_select_workspace(menu_item) {
                var workspace_id = menu_item.value
                update_cookie()
                if (workspace_id == "_new") {
                    do_open_workspace_dialog()
                } else {
                    var workspace = dm4c.fetch_topic(workspace_id)
                    dm4c.show_topic(workspace, "show", undefined, true)     // coordinates=undefined, do_center=true
                }
            }
        }

        function do_open_workspace_dialog() {
            var name_input = dm4c.render.input(undefined, 30)
            var type_selector = type_selector()
            dm4c.ui.dialog({
                title: "New Workspace",
                content: dm4c.render.label("Name").add(name_input)
                    .add(dm4c.render.label("Type")).add(type_selector),
                button_label: "Create",
                button_handler: do_create_workspace
            })

            function type_selector() {
                var selector = $()
                add_option("Private",       "dm4.workspaces.type.private", true)
                add_option("Confidential",  "dm4.workspaces.type.confidential")
                add_option("Collaborative", "dm4.workspaces.type.collaborative")
                add_option("Public",        "dm4.workspaces.type.public")
                add_option("Common",        "dm4.workspaces.type.common")
                return selector

                function add_option(label, value, checked) {
                    selector = selector
                        .add($("<label>")
                            .append($("<input>").attr({
                                type: "radio", name: "workspace-type", value: value, checked: checked
                            }))
                            .append(label)
                        )
                        .add($("<br>"))
                }
            }

            function do_create_workspace() {
                var name = name_input.val()
                var type_uri = type_selector.find(":checked").val()
                create_workspace(name, type_uri)
            }
        }
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        if (topic.type_uri == "dm4.workspaces.workspace") {
            refresh_workspace_menu()
        }
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_delete_topic", function(topic) {
        if (topic.type_uri == "dm4.workspaces.workspace") {
            refresh_workspace_menu()
        }
    })



    // === Access Control Listeners ===

    dm4c.add_listener("logged_in", function(username) {
        refresh_workspace_menu()
    })

    dm4c.add_listener("logged_out", function() {
        refresh_workspace_menu()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The ID of the selected workspace
     */
    this.get_workspace_id = function() {
        return get_workspace_id_from_menu()
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_all_workspaces() {
        return dm4c.restc.get_topics("dm4.workspaces.workspace", false, true).items  // fetch_composite=false, sort=true
    }

    /**
     * Reads out the workspace menu and returns the workspace ID.
     * If the workspace menu has no items yet, undefined is returned.
     */
    function get_workspace_id_from_menu() {
        var item = workspace_menu.get_selection()
        return item && item.value
    }

    // ---

    /**
     * Creates a workspace with the given name and type and puts it in the workspace menu.
     *
     * @param   type_uri    The URI of the workspace type ("dm4.workspaces.type.private",
     *                      "dm4.workspaces.type.confidential", ...)
     */
    function create_workspace(name, type_uri) {
        var workspace = create_workspace_topic(name, type_uri)
        refresh_workspace_menu(workspace.id)
    }

    /**
     * Creates a new workspace in the DB.
     */
    function create_workspace_topic(name, type_uri) {
        return dm4c.create_topic("dm4.workspaces.workspace", {
            "dm4.workspaces.name": name,
            "dm4.workspaces.type": "ref_uri:" + type_uri
        })
    }

    // ---

    /**
     * ### TODO: refactor this method. Currently all 3 aspects are mixed-up: DB-update, viewmodel update,
     * and view update. Compare to refresh_topicmap_menu() in Topicmaps plugin.
     *
     * @param   workspace_id    Optional: ID of the workspace to select.
     *                          If not given, the current selection is preserved.
     */
    function refresh_workspace_menu(workspace_id, workspaces) {
        if (!workspace_id) {
            workspace_id = get_workspace_id_from_menu()
        }
        if (!workspaces) {
            workspaces = get_all_workspaces()
        }
        //
        workspace_menu.empty()
        var icon_src = dm4c.get_type_icon_src("dm4.workspaces.workspace")
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
})
