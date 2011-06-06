package de.deepamehta.core;

import org.codehaus.jettison.json.JSONObject;



public interface Role {

    String getRoleTypeUri();

    void setRoleTypeUri(String roleTypeUri);

    // ---

    Association getAssociation();

    // ---

    JSONObject toJSON();
}
