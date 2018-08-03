package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;



/**
 * Creates the "System" workspace.
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    @Inject
    private WorkspacesService wsService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        Topic systemWorkspace = wsService.createWorkspace(
            AccessControlService.SYSTEM_WORKSPACE_NAME,
            AccessControlService.SYSTEM_WORKSPACE_URI,
            AccessControlService.SYSTEM_WORKSPACE_SHARING_MODE
        );
        // Note: at migration running time our plugin listeners are not yet registered
        // (furthermore there is no user logged in). So we set the owner manually here.
        acService.setWorkspaceOwner(systemWorkspace, AccessControlService.ADMIN_USERNAME);
        // Note: we don't set a particular creator/modifier here as we don't want suggest that the System workspace has
        // been created by the "admin" user. Instead the creator/modifier of the System workspace remain undefined as
        // the System workspace is actually created by the system itself.
    }
}
