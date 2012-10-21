package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationTypeModel;



/**
 * An association type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationType extends AttachedType implements AssociationType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationType(AssociationTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === AttachedType Overrides ===

    @Override
    public AssociationTypeModel getModel() {
        return (AssociationTypeModel) super.getModel();
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    // === AttachedTopic Overrides ===

    @Override
    protected String className() {
        return "association type";
    }
}
