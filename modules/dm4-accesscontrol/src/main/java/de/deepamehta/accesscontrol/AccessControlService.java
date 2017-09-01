package de.deepamehta.accesscontrol;

import de.deepamehta.accesscontrol.AuthorizationMethod;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.accesscontrol.Credentials;
import de.deepamehta.core.service.accesscontrol.Permissions;
import de.deepamehta.core.service.accesscontrol.SharingMode;

import java.util.Collection;



public interface AccessControlService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Admin user account
    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_INITIAL_PASSWORD = System.getProperty("dm4.security.initial_admin_password", "");

    // Administration workspace
    static final String ADMINISTRATION_WORKSPACE_NAME = "Administration";
    static final String ADMINISTRATION_WORKSPACE_URI = "dm4.workspaces.administration";
    static final SharingMode ADMINISTRATION_WORKSPACE_SHARING_MODE = SharingMode.COLLABORATIVE;

    // System workspace
    static final String SYSTEM_WORKSPACE_NAME = "System";
    static final String SYSTEM_WORKSPACE_URI = "dm4.workspaces.system";
    static final SharingMode SYSTEM_WORKSPACE_SHARING_MODE = SharingMode.PUBLIC;

    // Private workspaces
    static final String DEFAULT_PRIVATE_WORKSPACE_NAME = "Private Workspace";

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === User Session ===

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
     * @return  The username, or <code>null</code> if no user is logged in.
     */
    String getUsername();

    /**
     * Returns the "Username" topic of the logged in user.
     *
     * @return  The "Username" topic (type <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no user is logged in.
     */
    Topic getUsernameTopic();

    // ---

    /**
     * Returns the private workspace of the logged in user.
     * If no user is logged in an exception is thrown.
     * <p>
     * Note: a user can have more than one private workspace. The workspace returned
     * by this method is the one that holds the user's password topic.
     */
    Topic getPrivateWorkspace();



    // === User Accounts ===

    /**
     * @return  The "Username" topic of the created user account.
     */
    Topic createUserAccount(Credentials cred);

    /**
     * Returns the "Username" topic for the specified username.
     *
     * @param   username    a username. Must not be null.
     *
     * @return  The "Username" topic (type <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no such username exists.
     */
    Topic getUsernameTopic(String username);



    // === Workspaces / Memberships ===

    /**
     * Returns the owner of a workspace.
     *
     * @return  The username of the owner, or <code>null</code> if no owner is set.
     *          ### TODO: should throw an exception instead of returning null
     */
    String getWorkspaceOwner(long workspaceId);

    /**
     * Sets the owner of a workspace.
     * ### TODO: should take an ID instead a topic.
     * ### Core service must be extended with a property setter.
     */
    void setWorkspaceOwner(Topic workspace, String username);

    // ---

    void createMembership(String username, long workspaceId);

    /**
     * Checks if a user is a member of the given workspace.
     *
     * @param   username        the user.
     *                          If <code>null</code> is passed, <code>false</code> is returned.
     *                          If an unknown username is passed an exception is thrown.
     * @param   workspaceId     the workspace.
     *
     * @return  <code>true</code> if the user is a member, <code>false</code> otherwise.
     */
    boolean isMember(String username, long workspaceId);



    // === Permissions ===

    /**
     * @return  A Permissions object with one entry: <code>dm4.accesscontrol.operation.write</code>.
     */
    Permissions getTopicPermissions(long topicId);

    /**
     * @return  A Permissions object with one entry: <code>dm4.accesscontrol.operation.write</code>.
     */
    Permissions getAssociationPermissions(long assocId);



    // === Object Info ===

    /**
     * Returns the creator of a topic or an association.
     *
     * @return  The username of the creator, or <code>null</code> if no creator is set.
     */
    String getCreator(long objectId);

    /**
     * Returns the modifier of a topic or an association.
     *
     * @return  The username of the modifier, or <code>null</code> if no modifier is set.
     */
    String getModifier(long objectId);



    // === Retrieval ===

    Collection<Topic> getTopicsByCreator(String username);

    Collection<Topic> getTopicsByOwner(String username);

    Collection<Association> getAssociationsByCreator(String username);

    Collection<Association> getAssociationsByOwner(String username);



    // === Misc ===

    void registerAuthorizationMethod(String name, AuthorizationMethod am);

}
