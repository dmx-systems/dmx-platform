package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;



public interface AccessControlService extends PluginService {

    /**
     * Checks weather the credentials in the authorization string match an existing User Account,
     * and if so, creates an HTTP session. ### FIXDOC
     *
     * @param   authHeader  the authorization string containing the credentials. ### FIXDOC
     *                      Formatted like a "Authorization" HTTP header value. That is, "Basic " appended by the
     *                      Base64 encoded form of "{username}:{password}".
     *
     * @return  ### FIXDOC: The username of the matched User Account (a Topic of type "Username" /
     *          <code>dm4.accesscontrol.username</code>), or <code>null</code> if there is no matching User Account.
     */
    void login();

    /**
     * Logs the user out. That is invalidating the session associated with the JSESSION ID cookie.
     *
     * For a "non-private" DM installation the response is 204 No Content.
     * For a "private" DM installation the response is 401 Authorization Required. In this case the webclient is
     * supposed to shutdown the DM GUI then. The webclient of a "private" DM installation must only be visible/usable
     * when logged in.
     */
    void logout();

    // ---

    /**
     * Returns the username of the logged in user.
     *
     * @return  The username (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no user is logged in. ### FIXDOC
     */
    String getUsername();

    /**
     * Fetches the "Username" topic for the specified username.
     *
     * @return  The fetched Username (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no such user exists.
     */
    Topic getUsername(String username);

    // ---

    Permissions getTopicPermissions(long topicId);

    // ---

    /**
     * Fetches the creator of an object.
     *
     * @return  The creator (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no creator is set. ### FIXDOC
     */
    String getCreator(long objectId);

    /**
     * Assigns the specified user as the creator of the specified object.
     */
    void setCreator(long objectId, String username);

    // ---

    /**
     * Fetches the owner of an object.
     *
     * @return  The owner (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no owner is set. ### FIXDOC
     */
    String getOwner(long objectId);

    /**
     * Assigns the specified user as the owner of the specified object.
     */
    void setOwner(long objectId, String username);

    // ---

    /**
     * Fetches the Access Control List for the specified topic or association.
     * If no one is stored an empty Access Control List is returned.
     */
    AccessControlList getACL(long objectId);

    /**
     * Stores the Access Control List for the specified topic or association.
     */
    void setACL(long objectId, AccessControlList acl);

    // ---

    void joinWorkspace(String username, long workspaceId);
    void joinWorkspace(Topic  username, long workspaceId);
}
