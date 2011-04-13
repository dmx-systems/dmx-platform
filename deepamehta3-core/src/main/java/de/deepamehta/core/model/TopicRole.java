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

    private boolean topicIdentifiedByUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicRole(long topicId, String roleTypeUri) {
        this.topicId = topicId;
        this.topicUri = null;
        this.roleTypeUri = roleTypeUri;
        this.topicIdentifiedByUri = false;
    }

    public TopicRole(String topicUri, String roleTypeUri) {
        this.topicId = -1;
        this.topicUri = topicUri;
        this.roleTypeUri = roleTypeUri;
        this.topicIdentifiedByUri = true;
    }

    public TopicRole(JSONObject topicRole) {
        try {
            this.topicId = topicRole.optLong("topic_id", -1);
            this.topicUri = topicRole.optString("topic_uri", null);
            this.roleTypeUri = topicRole.getString("role_type_uri");
            this.topicIdentifiedByUri = topicUri != null;
            //
            if (topicId == -1 && topicUri == null) {
                throw new IllegalArgumentException("Neiter \"topic_id\" nor \"topic_uri\" is set");
            }
            if (topicId != -1 && topicUri != null) {
                throw new IllegalArgumentException("\"topic_id\" and \"topic_uri\" must not be set at the same time");
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicRole failed (JSONObject=" + topicRole + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getTopicId() {
        if (topicIdentifiedByUri) {
            throw new IllegalStateException("The topic is not identified by ID but by URI (" + this + ")");
        }
        return topicId;
    }

    public String getTopicUri() {
        if (!topicIdentifiedByUri) {
            throw new IllegalStateException("The topic is not identified by URI but by ID (" + this + ")");
        }
        return topicUri;
    }

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    public boolean topicIdentifiedByUri() {
        return topicIdentifiedByUri;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            if (topicIdentifiedByUri) {
                o.put("topic_uri", topicUri);
            } else {
                o.put("topic_id", topicId);
            }
            o.put("role_type_uri", roleTypeUri);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n        topic role (roleTypeUri=\"" + roleTypeUri + "\", topicId=" + topicId +
            ", topicUri=\"" + topicUri + "\", topicIdentifiedByUri=" + topicIdentifiedByUri + ")";
    }
}
