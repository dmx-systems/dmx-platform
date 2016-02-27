package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



abstract class RoleImpl implements Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModelImpl model;
    private Association assoc;  // the association this role is involved in

    PersistenceLayer pl;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RoleImpl(RoleModelImpl model, Association assoc, PersistenceLayer pl) {
        this.model = model;
        this.assoc = assoc;
        this.pl = pl;
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

    // Note: getPlayer() remains abstract

    // ---

    @Override
    public void setRoleTypeUri(String roleTypeUri) {
        model.updateRoleTypeUri(roleTypeUri);
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
