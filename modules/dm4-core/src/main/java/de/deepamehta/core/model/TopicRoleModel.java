package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * The role a topic plays in an association.
 * <p>
 * A TopicRoleModel object is a pair of a topic reference and a role type reference.
 * The topic is refered to either by its ID or URI.
 * The role type is refered to by its URI.
 * <p>
 * Assertion: both, the topic reference and the role type reference are set.
 * <p>
 * In the database a role type is represented by a topic of type "dm4.core.role_type".
 */
public class TopicRoleModel extends RoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicUri;
    private boolean topicIdentifiedByUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicRoleModel(long topicId, String roleTypeUri) {
        super(topicId, roleTypeUri);
        this.topicUri = null;
        this.topicIdentifiedByUri = false;
    }

    public TopicRoleModel(String topicUri, String roleTypeUri) {
        super(-1, roleTypeUri);
        this.topicUri = topicUri;
        this.topicIdentifiedByUri = true;
    }

    public TopicRoleModel(JSONObject topicRoleModel) {
        try {
            this.playerId = topicRoleModel.optLong("topic_id", -1);
            this.topicUri = topicRoleModel.optString("topic_uri", null);
            this.roleTypeUri = topicRoleModel.getString("role_type_uri");
            this.topicIdentifiedByUri = topicUri != null;
            //
            if (playerId == -1 && topicUri == null) {
                throw new IllegalArgumentException("Neiter \"topic_id\" nor \"topic_uri\" is set");
            }
            if (playerId != -1 && topicUri != null) {
                throw new IllegalArgumentException("\"topic_id\" and \"topic_uri\" must not be set at the same time");
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicRoleModel failed (JSONObject=" + topicRoleModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public long getPlayerId() {
        if (topicIdentifiedByUri) {
            throw new IllegalStateException("The topic is not identified by ID but by URI (" + this + ")");
        }
        return super.getPlayerId();
    }

    public String getTopicUri() {
        if (!topicIdentifiedByUri) {
            throw new IllegalStateException("The topic is not identified by URI but by ID (" + this + ")");
        }
        return topicUri;
    }

    public boolean topicIdentifiedByUri() {
        return topicIdentifiedByUri;
    }

    // === Implementation of abstract RoleModel methods ===

    @Override
    public boolean refsSameObject(RoleModel model) {
        if (model instanceof TopicRoleModel) {
            TopicRoleModel topicRole = (TopicRoleModel) model;
            if (topicRole.topicIdentifiedByUri == topicIdentifiedByUri) {
                if (topicIdentifiedByUri) {
                    return topicRole.topicUri.equals(topicUri);
                } else {
                    return topicRole.playerId == playerId;
                }
            }
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            if (topicIdentifiedByUri) {
                o.put("topic_uri", topicUri);
            } else {
                o.put("topic_id", playerId);
            }
            o.put("role_type_uri", roleTypeUri);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // === Java API ===

    @Override
    public String toString() {
        String player = topicIdentifiedByUri ? "topicUri=\"" + topicUri + "\"" : "playerId=" + playerId;
        return "\n        topic role (roleTypeUri=\"" + roleTypeUri + "\", " + player + ")";
    }
}
