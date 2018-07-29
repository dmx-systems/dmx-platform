package systems.dmx.core;

import systems.dmx.core.DMXObject;
import systems.dmx.core.model.RoleModel;



public interface Role extends JSONEnabled {

    String getRoleTypeUri();

    long getPlayerId();

    DMXObject getPlayer();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    RoleModel getModel();
}
