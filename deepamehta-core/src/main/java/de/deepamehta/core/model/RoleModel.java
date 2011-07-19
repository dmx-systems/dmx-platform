package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public abstract class RoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected String roleTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected RoleModel() {
    }

    protected RoleModel(String roleTypeUri) {
        this.roleTypeUri = roleTypeUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    public void setRoleTypeUri(String roleTypeUri) {
        this.roleTypeUri = roleTypeUri;
    }

    // ---

    public abstract JSONObject toJSON();
}
