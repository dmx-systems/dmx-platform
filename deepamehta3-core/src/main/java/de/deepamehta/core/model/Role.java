package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicUri;
    private String roleTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Role(JSONObject role) {
        try {
            this.topicUri = role.getString("topic");
            this.roleTypeUri = role.getString("role_type");
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getTopicUri() {
        return topicUri;
    }

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    // ---

    @Override
    public String toString() {
        return "role \"" + roleTypeUri + "\" (topicUri=\"" + topicUri + "\")";
    }
}
