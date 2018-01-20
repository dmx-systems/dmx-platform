package de.deepamehta.core.service.accesscontrol;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.concurrent.Callable;



public interface AccessControl {



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
     *
     * @param   cred    the username and new password.
     *                  An user account with the given username must exist. (The username can't be changed.)
     */
    void changePassword(Credentials cred);

    // ---

    /**
     * Returns the Username topic that corresponds to a username.
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
     * This is a privileged method, it bypasses the access control system.
     *
     * @throws  RuntimeException    if the user has no private workspace.
     *
     * @return  The user's private workspace (a topic of type "Workspace").
     */
    Topic getPrivateWorkspace(String username);

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

    String username(HttpSession session);



    // === Workspaces / Memberships ===

    /**
     * Returns a workspace by URI.
     * <p>
     * This is a privileged method: it works also if the current user has no READ permission for the workspace.
     *
     * @return  The workspace (a topic of type "Workspace").
     *
     * @throws  RuntimeException    If no workspace exists for the given URI.
     */
    Topic getWorkspace(String uri);

    // ---

    /**
     * Returns the ID of the "DeepaMehta" workspace.
     */
    long getDeepaMehtaWorkspaceId();

    /**
     * Returns the ID of the "Administration" workspace.
     */
    long getAdministrationWorkspaceId();

    /**
     * Returns the ID of the "System" workspace.
     */
    long getSystemWorkspaceId();

    // ---

    /**
     * Returns the ID of the workspace a topic or association is assigned to.
     *
     * @param   objectId    a topic ID, or an association ID
     *
     * @return  The workspace ID, or <code>-1</code> if no workspace is assigned.
     */
    long getAssignedWorkspaceId(long objectId);

    /**
     * Performs the initial workspace assignment for an object.
     * <p>
     * Use this method only for objects which have no workspace assignment already, that is e.g. objects
     * created in a migration or objects created while workspace assignment is deliberately suppressed.
     */
    void assignToWorkspace(DeepaMehtaObject object, long workspaceId);

    /**
     * Checks if an association represents a workspace assignment.
     * <p>
     * This is a privileged method: it works also if the current user has no READ permission for the potential
     * workspace.
     */
    boolean isWorkspaceAssignment(Association assoc);

    // ---

    /**
     * Runs a code block while suppressing the standard workspace assignment for all topics/associations
     * created within that code block.
     */
    <V> V runWithoutWorkspaceAssignment(Callable<V> callable) throws Exception;

    /**
     * Returns true if standard workspace assignment is currently suppressed for the current thread.
     */
    boolean workspaceAssignmentIsSuppressed();



    // === Topicmaps ===

    void deleteAssociationMapcontext(Association assoc);



    // === Config Service ===

    /**
     * Returns the configuration topic of the given type for the given topic.
     * <p>
     * This is a privileged method, it bypasses the access control system.
     *
     * @throws  RuntimeException    if no such configuration topic exists.
     */
    RelatedTopic getConfigTopic(String configTypeUri, long topicId);



    // === Email Addresses ===

    /**
     * Returns the username for the given email address.
     * <p>
     * The username is determined by traversing from the Email Address topic along a
     * <code>org.deepamehta.signup.user_mailbox</code> association.
     * <p>
     * This is a privileged method, it bypasses the access control system.
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
     * <code>org.deepamehta.signup.user_mailbox</code> association.
     * <p>
     * This is a privileged method, it bypasses the access control system.
     *
     * @throws  RuntimeException    if no such Username topic exists in the DB, or
     *                              if the Username topic is not associated to an Email Address topic.
     */
    String getEmailAddress(String username);

    // ---

    /**
     * Returns true if an "Email Address" (dm4.contacts.email_address) topic with the given value exists,
     * false otherwise.
     * <p>
     * This is a privileged method, it bypasses the access control system.
     */
    boolean emailAddressExists(String emailAddress);
}
