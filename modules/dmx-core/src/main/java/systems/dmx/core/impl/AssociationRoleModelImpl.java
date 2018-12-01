package systems.dmx.core.impl;

import systems.dmx.core.Role;
import systems.dmx.core.model.AssociationRoleModel;

import org.codehaus.jettison.json.JSONObject;



class AssociationRoleModelImpl extends RoleModelImpl implements AssociationRoleModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationRoleModelImpl(long assocId, String roleTypeUri, PersistenceLayer pl) {
        super(assocId, roleTypeUri,  pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of abstract RoleModel methods ===

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("assocId", playerId)       // TODO: call getPlayerId() but results in endless recursion if thwows
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
        return mf.newRelatedAssociationModel(pl.fetchAssociation(getPlayerId()), assoc);
    }
}
