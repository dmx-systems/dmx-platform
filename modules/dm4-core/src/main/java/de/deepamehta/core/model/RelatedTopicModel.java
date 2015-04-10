package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;



public class RelatedTopicModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopicModel(String topicUri, String topicTypeUri) {
        super(topicUri, topicTypeUri);
        this.relatingAssoc = new AssociationModel();
    }

    public RelatedTopicModel(String topicUri, AssociationModel relatingAssoc) {
        super(topicUri, (String) null);     // typeUri=null
        this.relatingAssoc = relatingAssoc;
    }

    public RelatedTopicModel(long topicId) {
        super(topicId);
        this.relatingAssoc = new AssociationModel();
    }

    public RelatedTopicModel(long topicId, AssociationModel relatingAssoc) {
        super(topicId);
        this.relatingAssoc = relatingAssoc;
    }

    public RelatedTopicModel(TopicModel topic) {
        super(topic);
        this.relatingAssoc = new AssociationModel();
    }

    public RelatedTopicModel(TopicModel topic, AssociationModel relatingAssoc) {
        super(topic);
        this.relatingAssoc = relatingAssoc;
    }

    RelatedTopicModel(JSONObject relatedTopic) throws JSONException {
        super(relatedTopic);
        this.relatingAssoc = new AssociationModel(relatedTopic.getJSONObject("assoc"));
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public AssociationModel getRelatingAssociation() {
        return relatingAssoc;
    }

    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("assoc", relatingAssoc.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // === Java API ===

    @Override
    public RelatedTopicModel clone() {
        try {
            return (RelatedTopicModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a RelatedTopicModel failed", e);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ", relating " + relatingAssoc;
    }
}
