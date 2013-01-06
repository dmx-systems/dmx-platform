package de.deepamehta.core.model;



public class RelatedAssociationModel extends AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedAssociationModel(AssociationModel assoc, AssociationModel relatingAssoc) {
        super(assoc);
        this.relatingAssoc = relatingAssoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public AssociationModel getRelatingAssociationModel() {
        return relatingAssoc;
    }

    // === Java API ===

    @Override
    public String toString() {
        return super.toString() + ", relating " + relatingAssoc;
    }
}
