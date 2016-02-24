package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.model.RelatedAssociationModel;



/**
 * An Association-Association pair that is attached to the {@link DeepaMehtaService}.
 */
class RelatedAssociationImpl extends AssociationImpl implements RelatedAssociation {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association relatingAssoc;      // Attached object cache

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssociationImpl(RelatedAssociationModel model, PersistenceLayer pl) {
        super(model, pl);
        this.relatingAssoc = new AssociationImpl(model.getRelatingAssociation(), pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Association getRelatingAssociation() {
        return relatingAssoc;
    }

    @Override
    public RelatedAssociationModelImpl getModel() {
        return (RelatedAssociationModelImpl) super.getModel();
    }
}
