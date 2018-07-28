package de.deepamehta.core.impl;

import de.deepamehta.core.DMXObject;
import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



abstract class RoleImpl implements Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    RoleModelImpl model;            // underlying model

    AssociationModelImpl assoc;     // the association this role is involved in

    // ---------------------------------------------------------------------------------------------------- Constructors

    RoleImpl(RoleModelImpl model, AssociationModelImpl assoc) {
        this.model = model;
        this.assoc = assoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public String getRoleTypeUri() {
        return model.getRoleTypeUri();
    }

    @Override
    public long getPlayerId() {
        return model.getPlayerId();
    }

    @Override
    public DMXObject getPlayer() {
        return model.getPlayer(assoc).instantiate();    // ### TODO: permission check?
    }

    // ---

    @Override
    public void setRoleTypeUri(String roleTypeUri) {
        assoc.updateRoleTypeUri(model, roleTypeUri);
    }

    // ---

    @Override
    public RoleModel getModel() {
        return model;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }
}
