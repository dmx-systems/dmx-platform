package systems.dmx.workspaces.migrations;

import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;



/**
 * Create "DMX" workspace.
 * <p>
 * Part of DMX 5.0-beta-4
 * Runs ALWAYS
 */
public class Migration3 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService wsService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        wsService.createWorkspace(
            WorkspacesService.DMX_WORKSPACE_NAME,
            WorkspacesService.DMX_WORKSPACE_URI,
            WorkspacesService.DMX_WORKSPACE_SHARING_MODE
        );
        // Note 1: the workspace has no owner yet as the Access Control plugin is not yet activated (as it depends
        // on the Workspaces plugin). We set the owner in the Access Control migration #2.
        // Note 2: we can't postpone the creation of the "DMX" workspace to a Access Control migration as it
        // must be already available at Workspaces plugin activation time (as needed for the type introduction).
    }
}
