package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface AccessControlService extends PluginService {

    /**
     * Checks weather the credentials match an existing User Account, and if so, creates an HTTP session.
     *
     * @return  The username of the matched User Account (a Topic of type "Username" /
     *          <code>dm4.accesscontrol.username</code>), or <code>null</code> if there is no matching User Account.
     */
    Topic login(String username, String password);

    // ---

    /**
     * Looks up a user account by username. ### TODO: drop this method
     *
     * @return  the user account (a Topic of type "User Account" / <code>dm4.accesscontrol.user_account</code>),
     *          or <code>null</code> if no such user account exists.
     */
    Topic lookupUserAccount(String username);

    /**
     * Returns the username of the logged in user.
     *
     * @return  the username (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no user is logged in.
     */
    Topic getUsername();

    // ---

    Permissions getTopicPermissions(long topicId);

    // ---

    Topic getOwnedTopic(long userId, String typeUri);

    void setOwner(long topicId, long userId);

    // ---

    void createACLEntry(long topicId, Role role, Permissions permissions);
    void createACLEntry(Topic topic,  Role role, Permissions permissions);

    // ---

    void joinWorkspace(long workspaceId, long userId);
}
