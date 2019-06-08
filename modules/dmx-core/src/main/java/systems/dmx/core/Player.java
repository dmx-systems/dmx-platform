package systems.dmx.core;

import systems.dmx.core.model.PlayerModel;



public interface Player extends JSONEnabled {

    String getRoleTypeUri();

    long getId();

    DMXObject getPlayer();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    PlayerModel getModel();
}
