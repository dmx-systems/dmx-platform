package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.model.RelatedAssociationModel;



/**
 * An Association-Association pair that is attached to the {@link DeepaMehtaService}.
 */
class AttachedRelatedAssociation extends AttachedAssociation implements RelatedAssociation {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedRelatedAssociation(RelatedAssociationModel model, EmbeddedService dms) {
        super(model, dms);
        this.relatingAssoc = new AttachedAssociation(model.getRelatingAssociationModel(), dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Association getRelatingAssociation() {
        return relatingAssoc;
    }
}
