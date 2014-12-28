package de.deepamehta.core.service.accesscontrol;

import de.deepamehta.core.DeepaMehtaObject;



public interface AccessControl {

    boolean checkCredentials(Credentials cred);

    boolean hasPermission(String username, Operation operation, long objectId);

    boolean isMember(String username, long workspaceId);

    void assignToWorkspace(DeepaMehtaObject object, long workspaceId);
}
