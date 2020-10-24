package systems.dmx.core.impl;

import systems.dmx.core.Player;
import systems.dmx.core.model.TopicPlayerModel;

import org.codehaus.jettison.json.JSONObject;



class TopicPlayerModelImpl extends PlayerModelImpl implements TopicPlayerModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    String topicUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicPlayerModelImpl(long topicId, String roleTypeUri, AccessLayer al) {
        this(topicId, null, roleTypeUri, al);
    }

    TopicPlayerModelImpl(String topicUri, String roleTypeUri, AccessLayer al) {
        this(-1, topicUri, roleTypeUri, al);
    }

    TopicPlayerModelImpl(long topicId, String topicUri, String roleTypeUri, AccessLayer al) {
        super(topicId, roleTypeUri, al);
        this.topicUri = topicUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String getTopicUri() {
        if (topicUri == null) {
            throw new IllegalStateException("Player URI is not set in " + this);
        }
        return topicUri;
    }

    @Override
    public boolean topicIdentifiedByUri() {
        return topicUri != null;
    }



    // === Implementation of abstract PlayerModel methods ===

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("topicId", id)       // TODO: call getId() but results in endless recursion if thwows
                .put("topicUri", topicUri)
                .put("roleTypeUri", roleTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Implementation of abstract PlayerModelImpl methods ===

    @Override
    Player instantiate(AssocModelImpl assoc) {
        return new TopicPlayerImpl(this, assoc);
    }

    @Override
    RelatedTopicModelImpl getDMXObject(AssocModelImpl assoc) {
        return mf.newRelatedTopicModel(getDMXObject(), assoc);
    }
}
