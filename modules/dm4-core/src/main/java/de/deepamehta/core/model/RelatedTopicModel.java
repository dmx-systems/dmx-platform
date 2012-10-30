package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public class RelatedTopicModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel assoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopicModel(TopicModel topic, AssociationModel assoc) {
        super(topic);
        this.assoc = assoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public AssociationModel getAssociationModel() {
        return assoc;
    }

    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("assoc", assoc.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
