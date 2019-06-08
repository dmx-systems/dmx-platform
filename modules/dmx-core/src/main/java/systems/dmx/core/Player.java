package systems.dmx.core;

import systems.dmx.core.model.PlayerModel;



public interface Player extends JSONEnabled {

    long getId();

    String getRoleTypeUri();

    DMXObject getDMXObject();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    PlayerModel getModel();
}
