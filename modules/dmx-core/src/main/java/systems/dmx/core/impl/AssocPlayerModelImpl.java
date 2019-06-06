package systems.dmx.core.impl;

import systems.dmx.core.Player;
import systems.dmx.core.model.AssocPlayerModel;

import org.codehaus.jettison.json.JSONObject;



class AssocPlayerModelImpl extends PlayerModelImpl implements AssocPlayerModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocPlayerModelImpl(long assocId, String roleTypeUri, PersistenceLayer pl) {
        super(assocId, roleTypeUri,  pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of abstract PlayerModel methods ===

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



    // === Implementation of abstract PlayerModelImpl methods ===

    @Override
    Player instantiate(AssocModelImpl assoc) {
        return new AssocPlayerImpl(this, assoc);
    }

    @Override
    RelatedAssocModelImpl getPlayer(AssocModelImpl assoc) {
        return mf.newRelatedAssocModel(pl.fetchAssoc(getPlayerId()), assoc);
    }
}
