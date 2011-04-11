package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class AssociationRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long   assocId;
    private String roleTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationRole(long assocId, String roleTypeUri) {
        this.assocId = assocId;
        this.roleTypeUri = roleTypeUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getAssociationId() {
        return assocId;
    }

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    // ---

    @Override
    public String toString() {
        return "\n        association role (roleTypeUri=\"" + roleTypeUri + "\", assocId=" + assocId + ")";
    }
}
