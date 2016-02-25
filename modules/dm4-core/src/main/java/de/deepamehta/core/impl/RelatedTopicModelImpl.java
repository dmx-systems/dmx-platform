package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



class RelatedTopicModelImpl extends TopicModelImpl implements RelatedTopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModelImpl relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicModelImpl(TopicModel topic, AssociationModel relatingAssoc) {
        super(topic);
        this.relatingAssoc = (AssociationModelImpl) relatingAssoc;
    }

    RelatedTopicModelImpl(RelatedTopicModel relatedTopic) {
        super(relatedTopic);
        this.relatingAssoc = (AssociationModelImpl) relatedTopic.getRelatingAssociation();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationModelImpl getRelatingAssociation() {
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
