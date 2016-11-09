function WorkspacesPluginModel() {

    // --------------------------------------------------------------------------------------------------- Private State

    var workspaces              // All workspaces readable by the current user (array of topic-like objects)
    var selected_workspace_id   // ID of the selected workspace

    // ------------------------------------------------------------------------------------------------------ Public API

    this.get_workspaces = function() {return workspaces}

    this.init = init
    this.select_workspace = select_workspace
    this.get_selected_workspace_id = get_selected_workspace_id
    this.delete_workspace = delete_workspace
    this.fetch_workspaces = fetch_workspaces
    this.is_workspace_readable = is_workspace_readable
    this.get_first_workspace_id = get_first_workspace_id

    // ----------------------------------------------------------------------------------------------- Private Functions

    function init() {
        fetch_workspaces()
        //
        var topicmap_id = get_initial_topicmap_id()
        // if either one applies get the corresponding workspace, otherwise choose arbitrary workspace
        var workspace_id = topicmap_id && get_workspace_id_of_topicmap(topicmap_id) || get_first_workspace_id()
        set_selected_workspace(workspace_id)

        function get_initial_topicmap_id() {
            return dm4c.get_plugin("de.deepamehta.topicmaps")._get_initial_topicmap_id()
        }

        function get_workspace_id_of_topicmap(topicmap_id) {
            var workspace = dm4c.restc.get_assigned_workspace(topicmap_id)
            if (!workspace) {
                throw "WorkspacesPluginModelError: topicmap " + topicmap_id + " is not assigned to any workspace"
            }
            return workspace.id
        }
    }

    // ---

    /**
     * Updates the model to reflect the given workspace is now selected, and fires the "post_select_workspace" event.
     */
    function select_workspace(workspace_id) {
        set_selected_workspace(workspace_id)
        dm4c.fire_event("post_select_workspace", workspace_id)
    }

    /**
     * Updates the model to reflect the given workspace is now selected.
     * That includes setting a cookie and updating 1 model object ("selected_workspace_id").
     */
    function set_selected_workspace(workspace_id) {
        js.set_cookie("dm4_workspace_id", workspace_id)
        selected_workspace_id = workspace_id
    }

    function get_selected_workspace_id() {
        if (!selected_workspace_id) {
            throw "WorkspacesPluginModelError: no workspace is selected yet"
        }
        return selected_workspace_id
    }

    // ---

    /**
     * Updates the model to reflect the given workspace is now deleted.
     */
    function delete_workspace(workspace_id) {
        fetch_workspaces()
        // if the deleted workspace was the selected one select another workspace
        if (workspace_id == get_selected_workspace_id()) {
            select_workspace(get_first_workspace_id())
        }
    }

    // ---

    function fetch_workspaces() {
        workspaces = dm4c.restc.get_topics("dm4.workspaces.workspace", false, true)   // include_childs=false, sort=true
        // suppress System workspace from appearing in menu
        js.delete(workspaces, function(workspace) {
            return workspace.uri == "dm4.workspaces.system"
        })
    }

    function is_workspace_readable(workspace_id) {
        return js.includes(workspaces, function(workspace) {
            return workspace.id == workspace_id
        })
    }

    function get_first_workspace_id() {
        var workspace = workspaces[0]
        if (!workspace) {
            throw "WorkspacesPluginModelError: no workspace available"
        }
        return workspace.id
    }
}
// Enable debugging for dynamically loaded scripts:
//# sourceURL=workspaces_plugin_model.js
