package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.SharingMode;

import java.util.List;
import java.util.logging.Logger;



/**
 * Converts the user accounts.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 *
 * TODO: drop it
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
        List<Topic> userAccounts = dmx.getTopicsByType("dmx.accesscontrol.user_account");
        logger.info("########## Converting " + userAccounts.size() + " user accounts");
        for (Topic userAccount : userAccounts) {
            // compare to AccessControlPlugin.createUserAccount()
            ChildTopics childTopics = userAccount.getChildTopics();
            Topic usernameTopic = childTopics.getTopic("dmx.accesscontrol.username");
            Topic passwordTopic = childTopics.getTopic("dmx.accesscontrol.password");
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
        List<RelatedTopic> workspaces = usernameTopic.getRelatedTopics("dmx.core.aggregation", "dmx.core.parent",
            "dmx.core.child", "dmx.workspaces.workspace");
        logger.info("######## User \"" + username + "\" is member of " + workspaces.size() + " workspaces");
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
