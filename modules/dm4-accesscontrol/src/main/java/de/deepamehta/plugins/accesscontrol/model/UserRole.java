package de.deepamehta.plugins.accesscontrol.model;

import java.util.HashMap;
import java.util.Map;



public enum UserRole {

    CREATOR("dm4.accesscontrol.user_role.creator"),
    OWNER("dm4.accesscontrol.user_role.owner"),
    MEMBER("dm4.accesscontrol.user_role.member"),
    USER("dm4.accesscontrol.user_role.user"),
    EVERYONE("dm4.accesscontrol.user_role.everyone");

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static Map<String, UserRole> userRoles;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public final String uri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private UserRole(String uri) {
        this.uri = uri;
        // userRoles.put(uri, this);    // ### Doesn't compile: "illegal reference to static field from initializer".
                                        // ### Enum constants are initialzed before other static fields.
        put(this);                      // ### Lazy initialization outside the constructor solves it.
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // Called by JAX-RS container to create a UserRole from a @PathParam or @QueryParam
    public static UserRole fromString(String uri) {
        return fromUri(uri);
    }

    public static UserRole fromUri(String uri) {
        return userRoles.get(uri);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(UserRole role) {
        // ### must initialize lazily, see above
        if (userRoles == null) {
            userRoles = new HashMap<String, UserRole>();
        }
        //
        userRoles.put(role.uri, role);
    }
}
