package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.workspaces.WorkspacesService;

import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.accesscontrol.SharingMode;

import java.util.logging.Logger;



/**
 * Converts the user accounts.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 */
public class Migration8 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    @Inject
    private WorkspacesService wsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // Note: at migration running time our plugin listeners are not yet registered. That means
        // access control is not yet in effect. We have full READ/WRITE access to the database.
        ResultList<RelatedTopic> userAccounts = dms.getTopics("dm4.accesscontrol.user_account");
        logger.info("########## Converting " + userAccounts.getSize() + " user accounts");
        for (Topic userAccount : userAccounts) {
            // compare to AccessControlPlugin.createUserAccount()
            ChildTopics childTopics = userAccount.getChildTopics();
            Topic usernameTopic = childTopics.getTopic("dm4.accesscontrol.username");
            Topic passwordTopic = childTopics.getTopic("dm4.accesscontrol.password");
            //
            // 1) create private workspace
            Topic privateWorkspace = wsService.createWorkspace(AccessControlService.DEFAULT_PRIVATE_WORKSPACE_NAME,
                null, SharingMode.PRIVATE);
            String username = usernameTopic.getSimpleValue().toString();
            acService.setWorkspaceOwner(privateWorkspace, username);
            //
            // 2) assign user account and password to private workspace
            long privateWorkspaceId = privateWorkspace.getId();
            wsService.assignToWorkspace(userAccount, privateWorkspaceId);
            wsService.assignToWorkspace(passwordTopic, privateWorkspaceId);
            //
            // 3) create memberships
            createMemberships(usernameTopic);
            //
            // 4) assign username to "System" workspace
            Topic systemWorkspace = wsService.getWorkspace(AccessControlService.SYSTEM_WORKSPACE_URI);
            wsService.assignToWorkspace(usernameTopic, systemWorkspace.getId());
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createMemberships(Topic usernameTopic) {
        String username = usernameTopic.getSimpleValue().toString();
        ResultList<RelatedTopic> workspaces = usernameTopic.getRelatedTopics("dm4.core.aggregation", "dm4.core.parent",
            "dm4.core.child", "dm4.workspaces.workspace");
        logger.info("######## User \"" + username + "\" is member of " + workspaces.getSize() + " workspaces");
        for (RelatedTopic workspace : workspaces) {
            long workspaceId = workspace.getId();
            String owner = acService.getWorkspaceOwner(workspaceId);
            boolean isOwner = username.equals(owner);
            logger.info("##### Workspace \"" + workspace.getSimpleValue() + "\" (id=" + workspace.getId() +
                "), owner: " + owner + " -> create " + (isOwner ? "NO " : "") + "Membership");
            if (!isOwner) {
                acService.createMembership(username, workspaceId);
            }
            workspace.getRelatingAssociation().delete();
        }
    }
}
