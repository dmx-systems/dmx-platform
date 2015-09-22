package de.deepamehta.core.service.accesscontrol;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;



public interface AccessControl {

    boolean checkCredentials(Credentials cred);

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

    /**
     * Returns the ID of the "System" workspace.
     */
    long getSystemWorkspaceId();

    // ---

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
     * @return  the Username topic, or <code>null</code> if no such Username topic exists.
     */
    Topic getUsernameTopic(HttpServletRequest request);

    /**
     * Returns the username that is associated with a request.
     *
     * @return  the username, or <code>null</code> if no user is associated with the request.
     */
    String getUsername(HttpServletRequest request);

    String username(HttpSession session);
}
