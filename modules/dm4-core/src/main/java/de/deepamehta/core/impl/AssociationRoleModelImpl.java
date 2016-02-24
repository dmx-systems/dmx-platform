package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



class AssociationRoleModelImpl extends RoleModelImpl implements AssociationRoleModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationRoleModelImpl(long assocId, String roleTypeUri, PersistenceLayer pl) {
        super(assocId, roleTypeUri,  pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of abstract RoleModel methods ===

    @Override
    public boolean refsSameObject(RoleModel model) {
        if (model instanceof AssociationRoleModel) {
            AssociationRoleModel assocRole = (AssociationRoleModel) model;
            return assocRole.getPlayerId() == playerId;
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("assoc_id", playerId);
            o.put("role_type_uri", roleTypeUri);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public String toString() {
        return "\n        association role (roleTypeUri=\"" + roleTypeUri + "\", playerId=" + playerId + ")";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    AssociationModel getPlayer() {
        return pl.fetchAssociation(playerId);
    }
}
