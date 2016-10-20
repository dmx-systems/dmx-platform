dm4c.add_plugin("de.deepamehta.workspaces", function() {

    dm4c.load_script("/de.deepamehta.workspaces/script/plugin_model.js")
    dm4c.load_script("/de.deepamehta.workspaces/script/plugin_view.js")

    var model = new WorkspacesPluginModel()
    var view = new WorkspacesPluginView({
        get_workspaces:            model.get_workspaces,
        select_workspace:          model.select_workspace,
        get_selected_workspace_id: model.get_selected_workspace_id,
        create_workspace: create_workspace
    })



    // === REST Client Extension ===

    dm4c.restc.create_workspace = function(name, uri, sharing_mode_uri) {
        return this.request("POST", "/workspace/" + encodeURIComponent(name) + "/" + (uri || "") + "/" +
            sharing_mode_uri)
    }
    dm4c.restc.get_workspace = function(uri, include_childs) {
        var params = this.queryParams({include_childs: include_childs})
        return this.request("GET", "/workspace/" + uri + params)
    }
    dm4c.restc.get_assigned_topics = function(workspace_id, topic_type_uri, include_childs) {
        var params = this.queryParams({include_childs: include_childs})
        return this.request("GET", "/workspace/" + workspace_id + "/topics/" + topic_type_uri + params)
    }
    dm4c.restc.get_assigned_workspace = function(object_id, include_childs) {
        var params = this.queryParams({include_childs: include_childs})
        return this.request("GET", "/workspace/object/" + object_id + params)
    }
    dm4c.restc.assign_to_workspace = function(object_id, workspace_id) {
        this.request("PUT", "/workspace/" + workspace_id + "/object/" + object_id)
    }



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {
        // init model
        model.init()
        // init view
        view.create_workspace_widget()
        view.refresh_workspace_menu()
    })

    dm4c.add_listener("topic_commands", function(topic) {
        if (dm4c.has_write_permission_for_topic(topic.id) && topic.type_uri != "dm4.workspaces.workspace") {
            return [
                {context: "context-menu", is_separator: true},
                {context: "context-menu", label: "Assign to Workspace", handler: function() {
                    view.open_assign_workspace_dialog(topic.id, "Topic")
                }}
            ]
        }
    })

    dm4c.add_listener("association_commands", function(assoc) {
        if (dm4c.has_write_permission_for_association(assoc.id)) {
            return [
                {context: "context-menu", is_separator: true},
                {context: "context-menu", label: "Assign to Workspace", handler: function() {
                    view.open_assign_workspace_dialog(assoc.id, "Association")
                }}
            ]
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
            // 1) update model
            model.delete_workspace(topic.id)
            // 2) update view
            view.refresh_workspace_menu()
        }
    })



    // === Access Control Listeners ===

    dm4c.add_listener("logged_in", function(username) {
        fetch_workspaces_and_refresh_menu()
    })

    // Note: the Topicmaps plugin clears its topicmap cache at authority_decreased(1). Switching the workspace at
    // authority_decreased_2 ensures the Topicmaps plugin loads an up-to-date topicmap (in its "post_select_workspace"
    // listener).
    dm4c.add_listener("authority_decreased_2", function() {
        // 1) update model
        model.fetch_workspaces()
        //
        var selected_workspace_id = model.get_selected_workspace_id()
        if (model.is_workspace_readable(selected_workspace_id)) {
            // stay in selected workspace
            var workspace_id = selected_workspace_id
        } else {
            // switch to another workspace
            var workspace_id = model.get_first_workspace_id()
        }
        // Note: we must select a workspace in any case in order to fire the "post_select_workspace" event.
        // Even when we stay in the selected workspace the Topicmaps plugin must adjust the current topicmap
        // according to decreased authority.
        model.select_workspace(workspace_id)
        //
        // 2) update view
        view.refresh_workspace_menu()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * @return  The ID of the selected workspace
     */
    this.get_selected_workspace_id = function() {
        return model.get_selected_workspace_id()
    }

    /**
     * Selects a workspace programmatically.
     * The respective item from the workspace menu is selected and the workspace is displayed.
     */
    this.select_workspace = function(workspace_id) {
        // update model
        model.select_workspace(workspace_id)
        // update view
        view.refresh_menu_item()
    }

    this.get_workspace = function(uri, include_childs) {
        return dm4c.restc.get_workspace(uri, include_childs)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    // *************************
    // *** Controller Helper ***
    // *************************



    /**
     * Creates a workspace with the given name and sharing mode, puts it in the workspace menu, and selects it.
     *
     * @param   sharing_mode_uri    The URI of the sharing mode ("dm4.workspaces.private",
     *                              "dm4.workspaces.confidential", ...)
     */
    function create_workspace(name, sharing_mode_uri) {
        // update DB
        var workspace = create_workspace_topic(name, sharing_mode_uri)
        // update model + view
        add_workspace(workspace.id)
    }

    /**
     * Creates a new workspace (a topic of type "Workspace") in the DB.
     *
     * @return  The created Workspace topic.
     */
    function create_workspace_topic(name, sharing_mode_uri) {
        return dm4c.restc.create_workspace(name, undefined, sharing_mode_uri)   // uri=undefined
    }

    /**
     * Puts a new workspace in the workspace menu, and selects it.
     * This is called when a new workspace is created at server-side and now should be displayed.
     */
    function add_workspace(workspace_id) {
        // update model
        model.fetch_workspaces()
        model.select_workspace(workspace_id)
        // update view
        view.refresh_workspace_menu()
    }

    function fetch_workspaces_and_refresh_menu() {
        // update model
        model.fetch_workspaces()
        // update view
        view.refresh_workspace_menu()
    }
})
// Enable debugging for dynamically loaded scripts:
//# sourceURL=workspaces_plugin.js
