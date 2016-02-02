package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



class RelatedTopicModelImpl extends TopicModelImpl implements RelatedTopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicModelImpl(long topicId) {
        super(topicId);
        this.relatingAssoc = new AssociationModel();
    }

    RelatedTopicModelImpl(long topicId, AssociationModel relatingAssoc) {
        super(topicId);
        this.relatingAssoc = relatingAssoc;
    }

    RelatedTopicModelImpl(String topicUri) {
        super(topicUri, (String) null);     // topicTypeUri=null
        this.relatingAssoc = new AssociationModel();
    }

    RelatedTopicModelImpl(String topicUri, AssociationModel relatingAssoc) {
        super(topicUri, (String) null);     // topicTypeUri=null
        this.relatingAssoc = relatingAssoc;
    }

    RelatedTopicModelImpl(String topicTypeUri, SimpleValue value) {
        super(topicTypeUri, value);
        this.relatingAssoc = new AssociationModel();
    }

    RelatedTopicModelImpl(String topicTypeUri, ChildTopicsModel childTopics) {
        super(topicTypeUri, childTopics);
        this.relatingAssoc = new AssociationModel();
    }

    RelatedTopicModelImpl(TopicModel topic) {
        super(topic);
        this.relatingAssoc = new AssociationModel();
    }

    RelatedTopicModelImpl(TopicModel topic, AssociationModel relatingAssoc) {
        super(topic);
        this.relatingAssoc = relatingAssoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationModel getRelatingAssociation() {
        return relatingAssoc;
    }

    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            // Note: the relating association might be uninitialized and thus not serializable.
            // This is the case at least for enrichments which have no underlying topics (e.g. timestamps).
            // ### TODO: remodel enrichments? Don't put them in a child topics model but in a proprietary field?
            if (relatingAssoc.getRoleModel1() != null) {
                o.put("assoc", relatingAssoc.toJSON());
            }
            //
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
