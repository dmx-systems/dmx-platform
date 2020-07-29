package systems.dmx.core.model;

import systems.dmx.core.JSONEnabled;



/**
 * The data that underly a {@link Player}.
 */
public interface PlayerModel extends JSONEnabled, Cloneable {

    long getId();

    String getTypeUri();

    String getRoleTypeUri();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    PlayerModel clone();
}
