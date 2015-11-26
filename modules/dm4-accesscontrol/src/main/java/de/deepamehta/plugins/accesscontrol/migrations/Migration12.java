package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.config.ConfigService;
import de.deepamehta.plugins.workspaces.WorkspacesService;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ResultList;

import java.util.logging.Logger;



/**
 * Creates the "Administration" workspace.
 * Reassigns existing "Login enabled" and "Disk Quota" config topics to the "Administration" workspace.
 * Creates "Enabled Sharing Modes" config topics for existing usernames.
 * <p>
 * Runs only in UPDATE mode.
 * <p>
 * Note: when CLEAN_INSTALLing DM 4.8-SNAPSHOT the "Administration" workspace is already created in migration 4,
 * and the config topics are already assigned to the "Administration" workspace (as the Config service is already
 * in charge when the "admin" user account is created, in migration 10).
 * <p>
 * The "Administration" workspace must exist before the "admin" user account is created (migration 10).
 * <p>
 * Part of DM 4.8-SNAPSHOT
 */
public class Migration12 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private AccessControlService acService;
    @Inject private WorkspacesService wsService;
    @Inject private ConfigService configService;

    private long administrationWorkspaceId;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // 1) create "Administration" workspace (Note: there is a copy in migration 4)
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
        //
        // 2) reassign existing "Login enabled" and "Disk Quota" config topics
        administrationWorkspaceId = dms.getAccessControl().getAdministrationWorkspaceId();
        assignConfigTopics("dm4.accesscontrol.login_enabled");
        assignConfigTopics("dm4.files.disk_quota");
        // Note: the "Disk Quota" config topics are reassigned to the Administration workspace which is created
        // only here. The "Disk Quota" config type belongs to the Files plugin and the reassignment is supposed
        // to be performed by a Files migration. But the Files migrations run *before* the Access Control migrations
        // (as the Access Control plugin depends indirectly on the Files plugin).
        //
        // 3) create "Enabled Sharing Modes" config topics
        ResultList<RelatedTopic> usernames = dms.getTopics("dm4.accesscontrol.username");
        logger.info("########## Creating config topics of type \"dm4.workspaces.enabled_sharing_modes\" for " +
            usernames.getSize() + " usernames");
        for (Topic username : usernames) {
            configService.createConfigTopic("dm4.workspaces.enabled_sharing_modes", username);
        }
        // Note: the "Enabled Sharing Modes" config topics are assigned to the Administration workspace which is
        // created only here. The "Enabled Sharing Modes" config type belongs to the Workspaces plugin and the
        // config topics are supposed to be created by a Workspaces migration. But the Workspaces migrations run
        // *before* the Access Control migrations (as the Access Control plugin depends on the Workspaces plugin).
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void assignConfigTopics(String configTypeUri) {
        ResultList<RelatedTopic> configTopics = dms.getTopics(configTypeUri);
        logger.info("########## Reassigning " + configTopics.getSize() + " config topics of type \"" + configTypeUri +
            "\" to workspace \"Administration\"");
        for (Topic configTopic : configTopics) {
            wsService.assignToWorkspace(configTopic, administrationWorkspaceId);
        }
    }
}
