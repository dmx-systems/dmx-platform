package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



abstract class RoleImpl implements Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModel model;
    private Association assoc;  // the association this role is involved in

    protected final EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected RoleImpl(RoleModel model, Association assoc, EmbeddedService dms) {
        this.model = model;
        this.assoc = assoc;
        this.dms = dms;
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
        // update memory
        model.setRoleTypeUri(roleTypeUri);
        // update DB
        storeRoleTypeUri();
    }

    // ---

    @Override
    public RoleModel getModel() {
        return model;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        return getModel().toJSON();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void storeRoleTypeUri() {
        dms.storageDecorator.storeRoleTypeUri(assoc.getId(), getPlayerId(), getRoleTypeUri());
    }
}
