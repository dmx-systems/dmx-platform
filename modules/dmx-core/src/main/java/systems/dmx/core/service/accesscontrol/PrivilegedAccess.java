package systems.dmx.core.service.accesscontrol;

import systems.dmx.core.Assoc;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.concurrent.Callable;



public interface PrivilegedAccess {



    // === Permissions ===

    /**
     * Checks if a user is permitted to perform an operation on an object (topic or association).
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     * @param   objectId    a topic ID, or an association ID.
     *
     * @return  <code>true</code> if permission is granted, <code>false</code> otherwise.
     */
    boolean hasPermission(String username, Operation operation, long objectId);

    // ---

    boolean hasReadPermission(String username, long workspaceId);

    boolean hasWritePermission(String username, long workspaceId);



    // === User Accounts ===

    /**
     * Checks if the given credentials are valid.
     *
     * @return  the corresponding Username topic if the credentials are valid, or <code>null</code> otherwise.
     */
    Topic checkCredentials(Credentials cred);

    /**
     * Changes the password of an existing user account.
     * <p>
     * This is a privileged method: it works also if the respective user is not logged in.
     * The latter is a requirement for a reset-password feature, as realized by the "DMX Sign-up" 3rd-party plugin.
     * (If a user forgot her password she is not logged in but still must be allowed to reset her password.)
     * <p>
     * Security: this method is neither called by the DMX platform itself, nor is it callable from outside as it has
     * no REST interface. So the DMX platform is still secure. On the other hand, a 3rd-party plugin which provides a
     * RESTful interface to this method is required to apply an additional authorization measure, e.g. a short-living
     * access token sent via email.
     *
     * @param   cred    the username and new password.
     *                  An user account with the given username must exist. (The username can't be changed.)
     */
    void changePassword(Credentials cred);

    /**
     * Creates a salt for the given credential's password, and
     * 1) stores the salt as a property of the given Password topic
     * 2) stores the salted password hash as the value of the given Password topic.
     *
     * @param   password    plain text
     */
    void storePasswordHash(Credentials cred, TopicModel passwordTopic);

    // ---

    /**
     * Returns the Username topic that corresponds to a username (case-insensitive).
     *
     * @return  the Username topic, or <code>null</code> if no such Username topic exists.
     */
    Topic getUsernameTopic(String username);

    /**
     * Returns the private workspace of the given user.
     * <p>
     * Note: a user can have more than one private workspace.
     * This method returns only the first one.
     * <p>
     * Access control is bypassed.
     *
     * @throws  RuntimeException    if the user has no private workspace.
     *
     * @return  The user's private workspace (a topic of type "Workspace").
     */
    Topic getPrivateWorkspace(String username);

    /**
     * Makes the given user a member of the given workspace.
     * <p>
     * This is a privileged method: the current user is <i>not</i> required to have WRITE permission for the given
     * workspace.
     */
    void createMembership(String username, long workspaceId);

    /**
     * Checks if a user is a member of a given workspace.
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    boolean isMember(String username, long workspaceId);

    /**
     * Returns the creator of a topic or an association.
     *
     * @return  The username of the creator, or <code>null</code> if no creator is set.
     */
    String getCreator(long objectId);



    // === Session ===

    /**
     * Returns the username that is associated with a request.
     *
     * @return  the username, or <code>null</code> if no user is associated with the request.
     */
    String getUsername(HttpServletRequest request);

    /**
     * Convenience method that returns the Username topic that corresponds to a request.
     * Basically it calls <code>getUsernameTopic(getUsername(request))</code>.
     *
     * @return  the Username topic, or <code>null</code> if no user is associated with the request.
     */
    Topic getUsernameTopic(HttpServletRequest request);

    /**
     * Returns the username that is associated with a session.
     *
     * @return  the username, or <code>null</code> if no user is associated with the session.
     */
    String username(HttpSession session);

    /**
     * Returns true if the running code was triggered from "outside", that is by a HTTP request, or from "inside",
     * that is while platform startup, in particular when an migration is running.
     *
     * @param   request     a request obtained via JAX-RS context injection, actually a proxy object which manages
     *                      thread-local request values. Must not be null.
     */
    boolean inRequestScope(HttpServletRequest request);



    // === Workspaces / Memberships ===

    /**
     * Fetches a Workspace topic by URI.
     * <p>
     * This is a privileged method: it works even if the current user has no READ permission for the workspace.
     *
     * @return  The Workspace topic.
     *
     * @throws  RuntimeException    if no workspace exists for the given URI.
     */
    Topic getWorkspace(String uri);

    // ---

    /**
     * Returns the ID of the "DMX" workspace.
     */
    long getDMXWorkspaceId();

    /**
     * Returns the ID of the "Administration" workspace.
     */
    long getAdminWorkspaceId();

    /**
     * Returns the ID of the "System" workspace.
     */
    long getSystemWorkspaceId();

    // ---

    /**
     * Returns the ID of the workspace a topic or association is assigned to.
     * <p>
     * Access control is bypassed.
     * READ permission is neither required for the given topic/association, nor for the returned workspace.
     *
     * @param   objectId    a topic ID, or an association ID
     *
     * @return  The workspace ID, or <code>-1</code> if no workspace is assigned.
     *
     * @throws  RuntimeException     if no object with the given ID exists.
     */
    long getAssignedWorkspaceId(long objectId);

    /**
     * Performs the initial workspace assignment for an object.
     * <p>
     * If the object is already assigned to the given workspace nothing is performed.
     * <p>
     * Note: this method can't be used to reassign an object to another workspace; use the
     * <code>WorkspacesService</code> instead. Typically this method is used for objects created in a migration or
     * objects created inside a <code>runInWorkspaceContext -1</code> context, or when the
     * <code>WorkspacesService</code> is not available for some reason.
     *
     * @throws  RuntimeException    if the object is already assigned to another workspace than the given workspace.
     */
    void assignToWorkspace(DMXObject object, long workspaceId);

    // ---

    // Note: thematically these 3 methods belong to the WorkspacesService. However they have moved to Core to avoid
    // cyclic dependencies. The Workspaces plugin depends on both the TopicmapsService and the ConfigService. Both
    // of them make use of runInWorkspaceContext() but can't depend on WorkspacesService.

    /**
     * Executes a code block and assigns all topics/associations created while that execution to the given workspace.
     * <p>
     * Use this method to override the standard workspace assignment (which is based on <code>dmx_workspace_id</code>
     * cookie or Workspace facet).
     * <p>
     * <code>runInWorkspaceContext()</code> calls can be nested.
     *
     * @param   workspaceId     the ID of the workspace the created topics/associations will be assigned to.
     *                          <p>
     *                          Pass <code>-1</code> to do no workspace assignments. In this case the topics/
     *                          associations are created without any workspace assignment. Consider using privileged
     *                          {@link #assignToWorkspace} to do the initial workspace assignments later on.
     *
     * @param   callable        the code block to execute.
     *
     * @return  The value returned by your <code>callable</code>.
     *
     * @throws  AccessControlException      if the current user has no WRITE permission for the given workspace.
     * @throws  IllegalArgumentException    if <code>workspaceId</code> does not refer to a Workspace.
     */
    <V> V runInWorkspaceContext(long workspaceId, Callable<V> callable) throws Exception;

    /**
     * Returns the workspace ID of the most recent {@link #runInWorkspaceContext} call in the current thread, or
     * <code>null</code> if there was no {@link #runInWorkspaceContext} call.
     */
    Long getWorkspaceContext();

    // ---

    /**
     * Deletes a Workspace topic and all its Memberships.
     * The current user needs WRITE permission to the workspace -- she must not necessarily be the workspace owner.
     * <p>
     * IMPORTANT: the actual workspace content is expected to be deleted already.
     * <p>
     * This is a privileged method for technical reasons: deleting a workspace topic involves deleting all its
     * Membership associations. As soon as the current user's membership is deleted she would, in case she is
     * not the workspace owner, have no permission anymore for deleting the Workspace topic eventually.
     */
    void deleteWorkspaceTopic(long workspaceId);



    // === Topicmaps ===

    void deleteAssocMapcontext(Assoc assoc);



    // === Config Service ===

    /**
     * Returns the configuration topic of the given type for the given topic.
     * <p>
     * Access control is bypassed.
     *
     * @throws  RuntimeException    if no such configuration topic exists.
     */
    RelatedTopic getConfigTopic(String configTypeUri, long topicId);



    // === Email Addresses ===

    /**
     * Returns the username for the given email address.
     * <p>
     * The username is determined by traversing from the Email Address topic along a
     * <code>dmx.base.user_mailbox</code> association.
     * <p>
     * Access control is bypassed.
     *
     * @throws  RuntimeException    if no such Email Address topic exists in the DB, or
     *                              if more than one such Email Address topics exist in the DB, or
     *                              if the Email Address topic is not associated to a Username topic.
     */
    String getUsername(String emailAddress);

    /**
     * Returns the email address for the given username.
     * <p>
     * The email address is determined by traversing from the Username topic along a
     * <code>dmx.base.user_mailbox</code> association.
     * <p>
     * Access control is bypassed.
     *
     * @throws  RuntimeException    if no such Username topic exists in the DB, or
     *                              if the Username topic is not associated to an Email Address topic.
     */
    String getEmailAddress(String username);

    // ---

    /**
     * Returns true if an "Email Address" (dmx.contacts.email_address) topic with the given value exists,
     * false otherwise.
     * <p>
     * The Email Address search is case-insensitive.
     * <p>
     * Access control is bypassed.
     */
    boolean emailAddressExists(String emailAddress);
}
