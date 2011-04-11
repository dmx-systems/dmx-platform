package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class TopicRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long   topicId;
    private String topicUri;
    private String roleTypeUri;

    private boolean topicIdentifiedById;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicRole(long topicId, String roleTypeUri) {
        this.topicId = topicId;
        this.roleTypeUri = roleTypeUri;
        this.topicIdentifiedById = true;
    }

    public TopicRole(String topicUri, String roleTypeUri) {
        this.topicUri = topicUri;
        this.roleTypeUri = roleTypeUri;
        this.topicIdentifiedById = false;
    }

    public TopicRole(JSONObject topicRole) {
        try {
            this.topicUri = topicRole.getString("topic_uri");
            this.roleTypeUri = topicRole.getString("role_type_uri");
            this.topicIdentifiedById = false;
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicRole failed (JSONObject=" + topicRole + ")", e);
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
        return "\n        topic role (roleTypeUri=\"" + roleTypeUri + "\", topicId=" + topicId +
            ", topicUri=\"" + topicUri + "\", topicIdentifiedById=" + topicIdentifiedById + ")";
    }
}
