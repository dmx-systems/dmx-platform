package de.deepamehta.core;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.model.RoleModel;



public interface Role extends JSONEnabled {

    String getRoleTypeUri();

    long getPlayerId();

    DeepaMehtaObject getPlayer();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    RoleModel getModel();
}
