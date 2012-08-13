package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface AccessControlService extends PluginService {

    /**
     * Checks weather the username and the plain text password match an existing User Account.
     *
     * Note: this is not a resource method (can't be invoked via HTTP) as plain text passwords are not supposed to be
     * send over the wire. This method is used by server-side plugins who process the credentials obtained through
     * Basic authorization (which sends the plain text password over the wire).
     *
     * @param   password    The plain text password.
     *
     * @return  The username of the matched User Account (a Topic of type "Username" /
     *          <code>dm4.accesscontrol.username</code>), or <code>null</code> if there is no matching User Account.
     */
    Topic login(String username, String password);

    /**
     * Looks up a user account by username.
     *
     * @return  a Topic of type "User Account" (<code>dm4.accesscontrol.user_account</code>),
     *          or <code>null</code> if no such user account exists.
     */
    Topic lookupUserAccount(String username);

    /**
     * Returns the username that is represented by the client state, or <code>null</code> if no user is logged in.
     *
     * @return  a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    Topic getUsername(ClientState clientState);

    // ---

    Permissions getTopicPermissions(long topicId, ClientState clientState);

    // ---

    Topic getOwnedTopic(long userId, String typeUri);

    void setOwner(long topicId, long userId);

    // ---

    void createACLEntry(long topicId, Role role, Permissions permissions);
    void createACLEntry(Topic topic,  Role role, Permissions permissions);

    // ---

    void joinWorkspace(long workspaceId, long userId);
}
