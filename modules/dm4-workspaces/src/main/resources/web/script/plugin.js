dm4c.add_plugin("de.deepamehta.workspaces", function() {

    var self = this

    // Model
    var selected_workspace_id   // ID of the selected workspace
    var workspaces              // All workspaces in the DB (topic-like objects)

    // View
    var workspace_menu          // A GUIToolkit Menu object



    // === REST Client Extension ===

    dm4c.restc.get_assigned_topics = function(workspace_id, topic_type_uri, include_childs) {
        var params = this.createRequestParameter({include_childs: include_childs})
        return this.request("GET", "/workspace/" + workspace_id + "/topics/" + topic_type_uri +
            params.to_query_string()).items
    }



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {

        create_workspace_menu()
        init_model()    // Note: the workspace menu must already be created

        function create_workspace_menu() {
            // build workspace widget
            var workspace_label = $("<span>").attr("id", "workspace-label").text("Workspace")
            workspace_menu = dm4c.ui.menu(do_select_workspace)
            var workspace_widget = $("<div>").attr("id", "workspace-widget")
                .append(workspace_label)
                .append(workspace_menu.dom)
            // put in toolbar
            dm4c.toolbar.dom.prepend(workspace_widget)

            function do_select_workspace(menu_item) {
                var workspace_id = menu_item.value
                if (workspace_id == "_new") {
                    do_open_workspace_dialog()
                } else {
                    select_workspace(workspace_id)
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
            fetch_workspaces_and_refresh_menu()
        }
    })

    /**
     * @param   topic   a Topic object
     */
    dm4c.add_listener("post_delete_topic", function(topic) {
        if (topic.type_uri == "dm4.workspaces.workspace") {
            // update the workspace menu
            fetch_workspaces_and_refresh_menu()
            // if the deleted workspace was the selected workspace select another one
            if (topic.id == selected_workspace_id) {
                select_workspace(get_workspace_id_from_menu())
            }
        }
    })



    // === Access Control Listeners ===

    dm4c.add_listener("logged_in", function(username) {
        fetch_workspaces_and_refresh_menu()
    })

    dm4c.add_listener("logged_out", function() {
        fetch_workspaces_and_refresh_menu()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The ID of the selected workspace
     */
    this.get_workspace_id = function() {
        if (!selected_workspace_id) {
            throw "WorkspacesError: the selected workspace is unknown"
        }
        return selected_workspace_id
    }

    /**
     * Selects a workspace programmatically.
     * The respective item from the workspace menu is selected and the workspace is displayed.
     */
    this.select_workspace = function(workspace_id) {
        select_menu_item(workspace_id)
        select_workspace(workspace_id)
    }



    // ----------------------------------------------------------------------------------------------- Private Functions



    // *************************
    // *** Controller Helper ***
    // *************************



    /**
     * Updates the model to reflect the given workspace is now selected, and fires the "post_select_workspace" event.
     *
     * Prerequisite: the workspace menu already shows the selected workspace.
     */
    function select_workspace(workspace_id) {
        // update model
        set_selected_workspace(workspace_id)
        //
        dm4c.fire_event("post_select_workspace", workspace_id)
    }

    /**
     * Creates a workspace with the given name and type, puts it in the workspace menu, and selects it.
     *
     * @param   type_uri    The URI of the workspace type ("dm4.workspaces.type.private",
     *                      "dm4.workspaces.type.confidential", ...)
     */
    function create_workspace(name, type_uri) {
        // update DB
        var workspace = create_workspace_topic(name, type_uri)
        // update model + view
        add_workspace(workspace.id)
    }

    /**
     * Creates a new workspace (a topic of type "Workspace") in the DB.
     *
     * @return  The created Topicmap topic.
     */
    function create_workspace_topic(name, type_uri) {
        return dm4c.create_topic("dm4.workspaces.workspace", {
            "dm4.workspaces.name": name,
            "dm4.workspaces.type": "ref_uri:" + type_uri
        })
    }

    /**
     * Puts a new workspace in the workspace menu, and selects it.
     * This is called when a new workspace is created at server-side and now should be displayed.
     */
    function add_workspace(workspace_id) {
        fetch_workspaces_and_refresh_menu()     // update model + view
        self.select_workspace(workspace_id)     // update model + view
    }

    function fetch_workspaces_and_refresh_menu() {
        // update model
        fetch_workspaces()
        // update view
        refresh_workspace_menu()
    }



    // *************
    // *** Model ***
    // *************



    function init_model() {
        fetch_workspaces_and_refresh_menu()
        set_selected_workspace(get_workspace_id_from_menu())
    }

    /**
     * Updates the model to reflect the given workspace is now selected ("selected_workspace_id").
     */
    function set_selected_workspace(workspace_id) {
        js.set_cookie("dm4_workspace_id", workspace_id)
        selected_workspace_id = workspace_id
    }

    function fetch_workspaces() {
        workspaces = dm4c.restc.get_topics("dm4.workspaces.workspace", false, true).items   // include_childs=false
                                                                                            // sort=true
    }



    // ************
    // *** View ***
    // ************



    // === Workspace Menu ===

    /**
     * Refreshes the workspace menu based on the model ("workspaces").
     */
    function refresh_workspace_menu() {
        var icon_src = dm4c.get_type_icon_src("dm4.workspaces.workspace")
        var workspace_id = get_workspace_id_from_menu()     // save selection
        workspace_menu.empty()
        // add workspaces to menu
        for (var i = 0, workspace; workspace = workspaces[i]; i++) {
            workspace_menu.add_item({label: workspace.value, value: workspace.id, icon: icon_src})
        }
        // add "New..." to menu
        if (dm4c.has_read_permission("dm4.workspaces.workspace")) {   // ### TODO: who is allowed to create a workspace?
            workspace_menu.add_separator()
            workspace_menu.add_item({label: "New Workspace...", value: "_new", is_trigger: true})
        }
        // restore selection
        select_menu_item(workspace_id)
    }

    /**
     * Selects an item from the workspace menu.
     */
    function select_menu_item(workspace_id) {
        workspace_menu.select(workspace_id)
    }

    /**
     * Reads out the workspace menu and returns the workspace ID.
     * If the workspace menu has no items yet, undefined is returned.
     */
    function get_workspace_id_from_menu() {
        var item = workspace_menu.get_selection()
        return item && item.value
    }
})
