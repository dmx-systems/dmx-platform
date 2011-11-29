package de.deepamehta.core.model;



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
}
