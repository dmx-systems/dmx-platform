package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;

import java.util.Collection;



public interface AccessControlService extends PluginService {



    // === Session ===

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



    // === User ===

    /**
     * Returns the username of the logged in user.
     *
     * @return  The username, or <code>null</code> if no user is logged in.
     */
    String getUsername();

    /**
     * Returns the "Username" topic for the specified username.
     *
     * @return  The "Username" topic (type <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no such username exists.
     */
    Topic getUsername(String username);



    // === Permissions ===

    Permissions getTopicPermissions(long topicId);

    Permissions getAssociationPermissions(long assocId);



    // === Creator ===

    /**
     * Returns the creator of a topic or an association.
     *
     * @return  The username of the creator, or <code>null</code> if no creator is set.
     */
    String getCreator(DeepaMehtaObject object);

    /**
     * Sets the creator of a topic or an association.
     */
    void setCreator(DeepaMehtaObject object, String username);



    // === Owner ===

    /**
     * Returns the owner of a topic or an association.
     *
     * @return  The username of the owner, or <code>null</code> if no owner is set.
     */
    String getOwner(DeepaMehtaObject object);

    /**
     * Sets the owner of a topic or an association.
     */
    void setOwner(DeepaMehtaObject object, String username);



    // === Access Control List ===

    /**
     * Returns the Access Control List of a topic or an association.
     *
     * @return  The Access Control List. If no one was set an empty Access Control List is returned.
     */
    AccessControlList getACL(DeepaMehtaObject object);

    /**
     * Sets the Access Control List for a topic or an association.
     */
    void setACL(DeepaMehtaObject object, AccessControlList acl);



    // === Workspaces ===

    void joinWorkspace(String username, long workspaceId);
    void joinWorkspace(Topic  username, long workspaceId);



    // === Retrieval ===

    Collection<Topic> getTopicsByCreator(String username);

    Collection<Topic> getTopicsByOwner(String username);

    Collection<Association> getAssociationsByCreator(String username);

    Collection<Association> getAssociationsByOwner(String username);
}
