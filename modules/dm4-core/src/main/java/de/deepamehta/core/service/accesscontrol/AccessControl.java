package de.deepamehta.core.service.accesscontrol;



public interface AccessControl {

    boolean hasPermission(String username, Operation operation, long objectId);

    boolean isMember(String username, long workspaceId);
}
