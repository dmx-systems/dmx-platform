package systems.dmx.core.impl;

import systems.dmx.core.Player;
import systems.dmx.core.model.AssocPlayerModel;

import org.codehaus.jettison.json.JSONObject;



class AssocPlayerModelImpl extends PlayerModelImpl implements AssocPlayerModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocPlayerModelImpl(long assocId, String roleTypeUri, AccessLayer al) {
        super(assocId, roleTypeUri,  al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of abstract PlayerModel methods ===

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("assocId", id)       // TODO: call getId() but results in endless recursion if thwows
                .put("roleTypeUri", roleTypeUri)
                .put("assoc", object != null ? object.toJSON() : null);
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
    RelatedAssocModelImpl getDMXObject(AssocModelImpl assoc) {
        return mf.newRelatedAssocModel(getDMXObject(), assoc);
    }
}
