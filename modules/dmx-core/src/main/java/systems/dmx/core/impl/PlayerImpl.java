package systems.dmx.core.impl;

import systems.dmx.core.DMXObject;
import systems.dmx.core.Player;
import systems.dmx.core.model.PlayerModel;

import org.codehaus.jettison.json.JSONObject;



abstract class PlayerImpl implements Player {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    PlayerModelImpl model;      // underlying model

    AssocModelImpl assoc;       // the association this role is involved in

    // ---------------------------------------------------------------------------------------------------- Constructors

    PlayerImpl(PlayerModelImpl model, AssocModelImpl assoc) {
        this.model = model;
        this.assoc = assoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Player Implementation ===

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
    public PlayerModel getModel() {
        return model;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }
}
