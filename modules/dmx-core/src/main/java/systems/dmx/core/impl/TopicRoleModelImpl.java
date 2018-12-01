package systems.dmx.core.impl;

import systems.dmx.core.Role;
import systems.dmx.core.model.TopicRoleModel;

import org.codehaus.jettison.json.JSONObject;



class TopicRoleModelImpl extends RoleModelImpl implements TopicRoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    String topicUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicRoleModelImpl(long topicId, String roleTypeUri, PersistenceLayer pl) {
        super(topicId, roleTypeUri, pl);
        this.topicUri = null;
    }

    TopicRoleModelImpl(String topicUri, String roleTypeUri, PersistenceLayer pl) {
        super(-1, roleTypeUri, pl);
        this.topicUri = topicUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String getTopicUri() {
        if (topicUri == null) {
            throw new IllegalStateException("Topic player URI is not set (" + this + ")");
        }
        return topicUri;
    }

    @Override
    public boolean topicIdentifiedByUri() {
        return topicUri != null;
    }



    // === Implementation of abstract RoleModel methods ===

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("topicId", playerId)       // TODO: call getPlayerId() but results in endless recursion if thwows
                .put("topicUri", topicUri)
                .put("roleTypeUri", roleTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Implementation of abstract RoleModelImpl methods ===

    @Override
    Role instantiate(AssociationModelImpl assoc) {
        return new TopicRoleImpl(this, assoc);
    }

    @Override
    RelatedTopicModelImpl getPlayer(AssociationModelImpl assoc) {
        return mf.newRelatedTopicModel(pl.fetchTopic(getPlayerId()), assoc);
    }
}
