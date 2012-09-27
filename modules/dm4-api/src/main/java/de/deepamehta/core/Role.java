package de.deepamehta.core;

import de.deepamehta.core.model.RoleModel;
import org.codehaus.jettison.json.JSONObject;



public interface Role extends JSONEnabled {

    String getRoleTypeUri();

    void setRoleTypeUri(String roleTypeUri);

    // ---

    Association getAssociation();

    // ---

    RoleModel getModel();
}
