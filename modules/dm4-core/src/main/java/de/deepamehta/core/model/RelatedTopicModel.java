package de.deepamehta.core.model;



public class RelatedTopicModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopicModel(TopicModel topic, AssociationModel relatingAssoc) {
        super(topic);
        this.relatingAssoc = relatingAssoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public AssociationModel getRelatingAssociation() {
        return relatingAssoc;
    }

    // === Java API ===

    @Override
    public String toString() {
        return super.toString() + ", relating " + relatingAssoc;
    }
}
