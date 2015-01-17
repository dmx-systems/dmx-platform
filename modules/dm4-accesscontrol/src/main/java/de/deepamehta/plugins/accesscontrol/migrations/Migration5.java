package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Creates the "System" workspace.
 * Runs only when UPDATE.
 * <p>
 * Part of DM 4.5
 */
public class Migration5 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    @Inject
    private WorkspacesService wsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // compare to AccessControlPlugin.postInstall()
        Topic systemWorkspace = wsService.createWorkspace(
            AccessControlService.SYSTEM_WORKSPACE_NAME,
            AccessControlService.SYSTEM_WORKSPACE_URI,
            AccessControlService.SYSTEM_WORKSPACE_SHARING_MODE
        );
        // Note: at migration running time no user is logged in.
        // So we set the owner manually here.
        acService.setWorkspaceOwner(systemWorkspace, AccessControlService.ADMIN_USERNAME);
    }
}
