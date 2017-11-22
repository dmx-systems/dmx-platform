package de.deepamehta.core.impl;

import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;

import org.codehaus.jettison.json.JSONObject;



class TopicRoleModelImpl extends RoleModelImpl implements TopicRoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicUri;
    private boolean topicIdentifiedByUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicRoleModelImpl(long topicId, String roleTypeUri, PersistenceLayer pl) {
        super(topicId, roleTypeUri, pl);
        this.topicUri = null;
        this.topicIdentifiedByUri = false;
    }

    TopicRoleModelImpl(String topicUri, String roleTypeUri, PersistenceLayer pl) {
        super(-1, roleTypeUri, pl);
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
                o.put("topicUri", topicUri);
            } else {
                o.put("topicId", playerId);
            }
            o.put("roleTypeUri", roleTypeUri);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public String toString() {
        String player = topicIdentifiedByUri ? "topicUri=\"" + topicUri + "\"" : "topicId=" + playerId;
        return "topic role (roleTypeUri=\"" + roleTypeUri + "\", " + player + ")";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Implementation of abstract RoleModelImpl methods ===

    @Override
    Role instantiate(AssociationModelImpl assoc) {
        return new TopicRoleImpl(this, assoc);
    }

    @Override
    RelatedTopicModelImpl getPlayer(AssociationModelImpl assoc) {
        TopicModel topic;
        if (topicIdentifiedByUri) {
            topic = pl.fetchTopic("uri", new SimpleValue(topicUri));
        } else {
            topic = pl.fetchTopic(playerId);
        }
        return mf.newRelatedTopicModel(topic, assoc);
    }
}
