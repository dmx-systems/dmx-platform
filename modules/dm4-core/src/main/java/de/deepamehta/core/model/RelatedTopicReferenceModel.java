package de.deepamehta.core.model;



public class RelatedTopicReferenceModel extends TopicReferenceModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics) {
        super(topicId);
        this.relatingAssoc = new AssociationModel(relatingAssocChildTopics);
    }

    public RelatedTopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics) {
        super(topicUri);
        this.relatingAssoc = new AssociationModel(relatingAssocChildTopics);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public AssociationModel getRelatingAssociation() {
        return relatingAssoc;
    }

    // ---

    @Override
    public String toString() {
        return "related " + super.toString();
    }
}
