package de.deepamehta.plugins.accesscontrol.model;

import java.util.HashMap;
import java.util.Map;



public enum Role {

    CREATOR("dm4.accesscontrol.role_creator"),
    OWNER("dm4.accesscontrol.role_owner"),
    MEMBER("dm4.accesscontrol.role_member"),
    EVERYONE("dm4.accesscontrol.role_everyone");

    public final String uri;

    private static Map<String, Role> roles;

    private Role(String uri) {
        this.uri = uri;
        // roles.put(uri, this);    // ### Doesn't compile: "illegal reference to static field from initializer".
                                    // ### Enum constants are initialzed before other static fields.
        put(uri, this);             // ### Lazy initialization outside the constructor solves it.
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // Called by JAX-RS container to create a Role from a @PathParam or @QueryParam
    public static Role fromString(String uri) {
        return fromUri(uri);
    }

    public static Role fromUri(String uri) {
        return roles.get(uri);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(String uri, Role role) {
        // ### must initialize lazily, see above
        if (roles == null) {
            roles = new HashMap();
        }
        //
        roles.put(uri, role);
    }
}
