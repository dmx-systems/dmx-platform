package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long   topicId;
    private String topicUri;
    private String roleTypeUri;

    private boolean topicIdentifiedById;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Role(long topicId, String roleTypeUri) {
        this.topicId = topicId;
        this.roleTypeUri = roleTypeUri;
        this.topicIdentifiedById = true;
    }

    public Role(String topicUri, String roleTypeUri) {
        this.topicUri = topicUri;
        this.roleTypeUri = roleTypeUri;
        this.topicIdentifiedById = false;
    }

    public Role(JSONObject role) {
        try {
            this.topicUri = role.getString("topic");
            this.roleTypeUri = role.getString("role_type");
            this.topicIdentifiedById = false;
        } catch (Exception e) {
            throw new RuntimeException("Parsing Role failed (JSONObject=" + role + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getTopicId() {
        return topicId;
    }

    public String getTopicUri() {
        return topicUri;
    }

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    public boolean topicIdentifiedById() {
        return topicIdentifiedById;
    }

    // ---

    @Override
    public String toString() {
        return "role \"" + roleTypeUri + "\" (topicId=" + topicId + ", topicUri=\"" + topicUri + "\")";
    }
}
