package de.deepamehta.core.service.accesscontrol;

import de.deepamehta.core.DeepaMehtaObject;



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

    /**
     * Checks if a user is a member of a given workspace.
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    boolean isMember(String username, long workspaceId);

    void assignToWorkspace(DeepaMehtaObject object, long workspaceId);
}
