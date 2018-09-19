package systems.dmx.core.impl;

import systems.dmx.core.Role;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.AssociationRoleModel;
import systems.dmx.core.model.RoleModel;

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
            return new JSONObject()
                .put("assocId", playerId)
                .put("roleTypeUri", roleTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Implementation of abstract RoleModelImpl methods ===

    @Override
    Role instantiate(AssociationModelImpl assoc) {
        return new AssociationRoleImpl(this, assoc);
    }

    @Override
    RelatedAssociationModelImpl getPlayer(AssociationModelImpl assoc) {
        return mf.newRelatedAssociationModel(pl.fetchAssociation(playerId), assoc);
    }
}