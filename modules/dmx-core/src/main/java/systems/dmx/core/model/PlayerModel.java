package systems.dmx.core.model;

import systems.dmx.core.JSONEnabled;



public interface PlayerModel extends JSONEnabled, Cloneable {

    long getId();

    String getRoleTypeUri();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    PlayerModel clone();
}
