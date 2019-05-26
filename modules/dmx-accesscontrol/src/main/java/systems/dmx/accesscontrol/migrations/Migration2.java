package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;



/**
 * Create the "System" and "Administration" workspaces.
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 * <p>
 * Note: the "admin" user account is created in migration 6. At this moment both must exist, the "Login enabled"
 * config topic type (created in migration 5), and the "Administration" workspace (created here).
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
        // "System"
        Topic systemWorkspace = wsService.createWorkspace(
            AccessControlService.SYSTEM_WORKSPACE_NAME,
            AccessControlService.SYSTEM_WORKSPACE_URI,
            AccessControlService.SYSTEM_WORKSPACE_SHARING_MODE
        );
        acService.setWorkspaceOwner(systemWorkspace, AccessControlService.ADMIN_USERNAME);
        //
        // "Administration"
        Topic adminWorkspace = wsService.createWorkspace(
            AccessControlService.ADMINISTRATION_WORKSPACE_NAME,
            AccessControlService.ADMINISTRATION_WORKSPACE_URI,
            AccessControlService.ADMINISTRATION_WORKSPACE_SHARING_MODE
        );
        acService.setWorkspaceOwner(adminWorkspace, AccessControlService.ADMIN_USERNAME);
        //
        // Note 1: at migration running time our plugin listeners are not yet registered (furthermore there is no user
        // logged in). So we set the workspace owner manually here.
        // Note 2: we don't set a particular creator/modifier here as we don't want suggest the workspaces have been
        // created by the "admin" user. Instead the creator/modifier of the workspaces remain undefined as the
        // workspaces are actually created by the system itself.
    }
}
