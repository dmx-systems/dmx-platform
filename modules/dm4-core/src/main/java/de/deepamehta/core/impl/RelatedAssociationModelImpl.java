package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RelatedAssociationModel;



class RelatedAssociationModelImpl extends AssociationModelImpl implements RelatedAssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModelImpl relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssociationModelImpl(AssociationModel assoc, AssociationModel relatingAssoc) {
        super(assoc);
        this.relatingAssoc = (AssociationModelImpl) relatingAssoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationModelImpl getRelatingAssociation() {
        return relatingAssoc;
    }

    // === Java API ===

    @Override
    public String toString() {
        return super.toString() + ", relating " + relatingAssoc;
    }
}
