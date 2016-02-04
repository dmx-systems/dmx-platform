package de.deepamehta.core.impl;

import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;

import org.codehaus.jettison.json.JSONObject;



class TopicRoleModelImpl extends RoleModelImpl implements TopicRoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicUri;
    private boolean topicIdentifiedByUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicRoleModelImpl(long topicId, String roleTypeUri) {
        super(topicId, roleTypeUri);
        this.topicUri = null;
        this.topicIdentifiedByUri = false;
    }

    TopicRoleModelImpl(String topicUri, String roleTypeUri) {
        super(-1, roleTypeUri);
        this.topicUri = topicUri;
        this.topicIdentifiedByUri = true;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public long getPlayerId() {
        if (topicIdentifiedByUri) {
            throw new IllegalStateException("The topic is not identified by ID but by URI (" + this + ")");
        }
        return super.getPlayerId();
    }

    @Override
    public String getTopicUri() {
        if (!topicIdentifiedByUri) {
            throw new IllegalStateException("The topic is not identified by URI but by ID (" + this + ")");
        }
        return topicUri;
    }

    @Override
    public boolean topicIdentifiedByUri() {
        return topicIdentifiedByUri;
    }

    // === Implementation of abstract RoleModel methods ===

    @Override
    public boolean refsSameObject(RoleModel model) {
        if (model instanceof TopicRoleModel) {
            TopicRoleModel topicRole = (TopicRoleModel) model;
            if (topicRole.topicIdentifiedByUri() == topicIdentifiedByUri) {
                if (topicIdentifiedByUri) {
                    return topicRole.getTopicUri().equals(topicUri);
                } else {
                    return topicRole.getPlayerId() == playerId;
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
