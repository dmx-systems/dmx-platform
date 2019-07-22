package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;



/**
 * Create "System" and "Administration" workspaces.
 * Set owner of "System", "Administration", and "DMX" workspaces.
 * <p>
 * Part of DMX 5.0-beta-4
 * Runs ALWAYS
 * <p>
 * Note: the "admin" user account is created in migration 3. At this moment both must exist, the "Login enabled"
 * config topic type (created in migration 1), and the "Administration" workspace (created here).
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
        // Note 1: at migration time our plugin listeners are not yet registered (furthermore no user is logged in).
        // We set the workspace owners manually.
        // Note 2: we don't set a creator/modifier; we don't want suggest the workspaces have been created by the
        // "admin" user. Instead creator/modifier remain undefined as the workspaces are created by the system itself.
        //
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
        // "DMX"
        acService.setWorkspaceOwner(wsService.getWorkspace(WorkspacesService.DMX_WORKSPACE_URI),
            AccessControlService.ADMIN_USERNAME);
    }
}
