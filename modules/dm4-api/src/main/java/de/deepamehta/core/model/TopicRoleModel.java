package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



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

    private long   topicId;
    private String topicUri;

    private boolean topicIdentifiedByUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicRoleModel(long topicId, String roleTypeUri) {
        super(roleTypeUri);
        this.topicId = topicId;
        this.topicUri = null;
        this.topicIdentifiedByUri = false;
    }

    public TopicRoleModel(String topicUri, String roleTypeUri) {
        super(roleTypeUri);
        this.topicId = -1;
        this.topicUri = topicUri;
        this.topicIdentifiedByUri = true;
    }

    public TopicRoleModel(JSONObject topicRoleModel) {
        try {
            this.topicId = topicRoleModel.optLong("topic_id", -1);
            this.topicUri = topicRoleModel.optString("topic_uri", null);
            this.roleTypeUri = topicRoleModel.getString("role_type_uri");
            this.topicIdentifiedByUri = topicUri != null;
            //
            if (topicId == -1 && topicUri == null) {
                throw new IllegalArgumentException("Neiter \"topic_id\" nor \"topic_uri\" is set");
            }
            if (topicId != -1 && topicUri != null) {
                throw new IllegalArgumentException("\"topic_id\" and \"topic_uri\" must not be set at the same time");
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicRoleModel failed (JSONObject=" + topicRoleModel + ")", e);
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
                    return topicRole.topicId == topicId;
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
                o.put("topic_id", topicId);
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
        return "\n        topic role (roleTypeUri=\"" + roleTypeUri + "\", topicId=" + topicId +
            ", topicUri=\"" + topicUri + "\", topicIdentifiedByUri=" + topicIdentifiedByUri + ")";
    }
}
