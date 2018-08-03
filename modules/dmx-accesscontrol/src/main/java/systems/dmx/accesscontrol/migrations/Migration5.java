package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;



/**
 * Sets "admin" as the owner of the "DMX" workspace.
 * Runs ALWAYS.
 * <p>
 * Part of DMX 5.0
 */
public class Migration5 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    @Inject
    private WorkspacesService wsService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        acService.setWorkspaceOwner(wsService.getWorkspace(WorkspacesService.DMX_WORKSPACE_URI),
            AccessControlService.ADMIN_USERNAME);
        // Note: we don't set a particular creator/modifier here as we don't want suggest that the DMX
        // workspace has been created by the "admin" user. Instead the creator/modifier of the DeepaMehhta
        // workspace remain undefined as the DMX workspace is actually created by the system itself.
    }
}
