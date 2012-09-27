package de.deepamehta.core;

import de.deepamehta.core.model.RoleModel;



public interface Role extends JSONEnabled {

    String getRoleTypeUri();

    void setRoleTypeUri(String roleTypeUri);

    // ---

    Association getAssociation();

    // ---

    RoleModel getModel();
}
