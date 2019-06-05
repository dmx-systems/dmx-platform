package systems.dmx.core;

import systems.dmx.core.DMXObject;
import systems.dmx.core.model.PlayerModel;



public interface Player extends JSONEnabled {

    String getRoleTypeUri();

    long getPlayerId();

    DMXObject getPlayer();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    PlayerModel getModel();
}
