package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.workspaces.WorkspacesService;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ResultList;

import java.util.logging.Logger;



/**
 * Creates the "Administration" workspace and assigns existing config topics to it.
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

    @Inject
    private AccessControlService acService;

    @Inject
    private WorkspacesService wsService;

    long administrationWorkspaceId;

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
        // 2) assign config topics
        administrationWorkspaceId = dms.getAccessControl().getAdministrationWorkspaceId();
        assignConfigTopics("dm4.accesscontrol.login_enabled");
        assignConfigTopics("dm4.files.disk_quota");
        // Note: the Files plugin is activated before the Access Control plugin (as the latter waits for the former
        // due to dependencies). So, the "dm4.files.disk_quota" type exists already for sure.
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void assignConfigTopics(String configTypeUri) {
        ResultList<RelatedTopic> configTopics = dms.getTopics(configTypeUri, 0);
        logger.info("########## Reassign " + configTopics.getSize() + " config topics of type \"" + configTypeUri +
            "\" to workspace \"Administration\"");
        for (Topic configTopic : configTopics) {
            wsService.assignToWorkspace(configTopic, administrationWorkspaceId);
        }
    }
}
