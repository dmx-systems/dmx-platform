package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;



/**
 * Creates the "Administration" workspace.
 * Runs only in CLEAN_INSTALL mode.
 * <p>
 * Part of DM 4.8
 * <p>
 * Note: when UPDATEing to 4.8 the "Administration" workspace is created in migration 12.
 *
 * ----------
 *
 * Originally this migration created the "admin" user account (part of DM 4.5).
 * Run mode was CLEAN_INSTALL as well.
 * <p>
 * Now the "admin" user account is created in migration 10. At this moment both must exist, the "Login enabled"
 * config topic type (created in migration 9 as of DM 4.7), and the "Administration" workspace (created here).
 * <p>
 * Note: when UPDATEing to 4.5 the "admin" user account already exists.
 * It was created via postInstall() hook (which is obsolete in 4.5)
 */
public class Migration4 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    @Inject
    private WorkspacesService wsService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // Note: there is a copy in migration 12
        Topic systemWorkspace = wsService.createWorkspace(
            AccessControlService.ADMINISTRATION_WORKSPACE_NAME,
            AccessControlService.ADMINISTRATION_WORKSPACE_URI,
            AccessControlService.ADMINISTRATION_WORKSPACE_SHARING_MODE
        );
        // Note: at migration running time our plugin listeners are not yet registered
        // (furthermore there is no user logged in). So we set the owner manually here.
        acService.setWorkspaceOwner(systemWorkspace, AccessControlService.ADMIN_USERNAME);
        // Note: we don't set a particular creator/modifier here as we don't want suggest that the Administration
        // workspace has been created by the "admin" user. Instead the creator/modifier of the Administration
        // workspace remain undefined as the Administration workspace is actually created by the system itself.
    }
}
