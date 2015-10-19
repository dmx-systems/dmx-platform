package de.deepamehta.core.service.accesscontrol;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.concurrent.Callable;



public interface AccessControl {

    /**
     * Checks if the given credentials are valid.
     *
     * @return  the corresponding Username topic if the credentials are valid, or <code>null</code> otherwise.
     */
    Topic checkCredentials(Credentials cred);

    /**
     * Returns the "Login enabled" configuration for the given user account.
     *
     * @param   usernameTopic   the username whose account to be checked.
     *
     * @return  <code>true</code> if the user account is enabled, <code>false</code> otherwise.
     */
    boolean getLoginEnabled(Topic usernameTopic);

    /**
     * Checks if a user is permitted to perform an operation on an object (topic or association).
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     * @param   objectId    a topic ID, or an association ID.
     *
     * @return  <code>true</code> if permission is granted, <code>false</code> otherwise.
     */
    boolean hasPermission(String username, Operation operation, long objectId);



    // === Workspaces ===

    /**
     * Returns a workspace by URI.
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
     * Returns the ID of the "System" workspace.
     */
    long getSystemWorkspaceId();

    // ---

    /**
     * Checks if a user is a member of a given workspace.
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    boolean isMember(String username, long workspaceId);

    /**
     * Performs the initial workspace assignment for an object.
     * <p>
     * Use this method only for objects which have no workspace assignment already, that is e.g. objects
     * created in a migration or objects created while workspace assignment is deliberately suppressed.
     */
    void assignToWorkspace(DeepaMehtaObject object, long workspaceId);

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



    // === Usernames ===

    /**
     * Returns the Username topic that corresponds to a username.
     *
     * @return  the Username topic, or <code>null</code> if no such Username topic exists.
     */
    Topic getUsernameTopic(String username);

    /**
     * Convenience method that returns the Username topic that corresponds to a request.
     * Basically it calls <code>getUsernameTopic(getUsername(request))</code>.
     *
     * @return  the Username topic, or <code>null</code> if no user is associated with the request.
     */
    Topic getUsernameTopic(HttpServletRequest request);

    /**
     * Returns the username that is associated with a request.
     *
     * @return  the username, or <code>null</code> if no user is associated with the request.
     */
    String getUsername(HttpServletRequest request);

    String username(HttpSession session);



    // === Email Addresses ===

    /**
     * Returns true if an "Email Address" (dm4.contacts.email_address) topic with the given value exists,
     * false otherwise.
     * <p>
     * This is a privileged method, it runs as "System".
     */
    boolean emailAddressExists(String emailAddress);
}
