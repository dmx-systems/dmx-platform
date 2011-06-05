package de.deepamehta.core.impl.service;

import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



class AttachedRole implements Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModel model;

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected AttachedRole(RoleModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public String getRoleTypeUri() {
        return model.getRoleTypeUri();
    }

    @Override
    public void setRoleTypeUri(String roleTypeUri) {
        model.setRoleTypeUri(roleTypeUri);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        return getModel().toJSON();
    }



    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected RoleModel getModel() {
        return model;
    }
}
