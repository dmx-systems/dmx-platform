package de.deepamehta.plugins.accesscontrol.model;



public enum Role {

    CREATOR, OWNER, MEMBER, EVERYONE;

    public String s() {
        return name().toLowerCase();
    }

    // Called by JAX-RS container to create a Role from a @PathParam or @QueryParam
    public static Role fromString(String role) {
        return valueOf(role.toUpperCase());
    }
}
