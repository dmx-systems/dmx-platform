package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public abstract class Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected String roleTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected Role() {
    }

    protected Role(String roleTypeUri) {
        this.roleTypeUri = roleTypeUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    public abstract JSONObject toJSON();
}
