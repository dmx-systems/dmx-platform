package de.deepamehta.core.service.accesscontrol;

import java.util.HashMap;
import java.util.Map;



public enum WorkspaceType {

    // ### TODO: move to dm4.core namespace?
    PRIVATE("dm4.workspaces.type.private"),
    CONFIDENTIAL("dm4.workspaces.type.confidential"),
    COLLABORATIVE("dm4.workspaces.type.collaborative"),
    PUBLIC("dm4.workspaces.type.public"),
    COMMON("dm4.workspaces.type.common");

    // ---

    public static WorkspaceType fromUri(String uri) {
        WorkspaceType workspaceType = workspaceTypes.get(uri);
        if (workspaceType == null) {
            throw new RuntimeException("\"" + uri + "\" is an unexpected workspace type URI");
        }
        return workspaceType;
    }

    // ---

    private final String uri;

    private static Map<String, WorkspaceType> workspaceTypes;

    private WorkspaceType(String uri) {
        this.uri = uri;
        put(uri, this);
        // workspaceTypes.put(uri, this);   // ### "illegal reference to static field from initializer"
    }

    private void put(String uri, WorkspaceType workspaceType) {
        if (workspaceTypes == null) {
            workspaceTypes = new HashMap();
        }
        workspaceTypes.put(uri, workspaceType);
    }
}
