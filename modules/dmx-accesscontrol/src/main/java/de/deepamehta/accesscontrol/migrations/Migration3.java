package de.deepamehta.accesscontrol.migrations;

import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.workspaces.WorkspacesService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;



/**
 * Creates the "System" workspace.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.5
 */
public class Migration3 extends Migration {

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
