package de.deepamehta.core;

import de.deepamehta.core.DMXObject;
import de.deepamehta.core.model.RoleModel;



public interface Role extends JSONEnabled {

    String getRoleTypeUri();

    long getPlayerId();

    DMXObject getPlayer();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    RoleModel getModel();
}
