package de.deepamehta.workspaces.migrations;

import de.deepamehta.workspaces.WorkspacesService;

import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;



/**
 * Creates the "DeepaMehta" workspace.
 * Runs only in CLEAN_INSTALL mode.
 * <p>
 * Note: when UPDATEing to 4.5 the "DeepaMehta" workspace already exists.
 * It was created via postInstall() hook (which is obsolete in 4.5)
 * <p>
 * Part of DM 4.5
 */
public class Migration4 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService wsService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        wsService.createWorkspace(
            WorkspacesService.DEEPAMEHTA_WORKSPACE_NAME,
            WorkspacesService.DEEPAMEHTA_WORKSPACE_URI,
            WorkspacesService.DEEPAMEHTA_WORKSPACE_SHARING_MODE
        );
        // Note 1: the workspace has no owner yet as the Access Control plugin is not yet activated (as it depends
        // on the Workspaces plugin). We set the owner in the Access Control migration #7.
        // Note 2: we can't postpone the creation of the "DeepaMehta" workspace to a Access Control migration as it
        // must be already available at Workspaces plugin activation time (as needed for the type introduction).
    }
}
