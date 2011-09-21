package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationTypeModel;



/**
 * An association type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationType extends AttachedType implements AssociationType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationType(EmbeddedService dms) {
        super(dms);     // The model remains uninitialized.
                        // It is initialized later on through fetch().
    }

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

    // === AttachedType Overrides ===

    @Override
    protected void putInTypeCache() {
        dms.typeCache.put(this);
    }

    // === AttachedTopic Overrides ===

    @Override
    protected String className() {
        return "association type";
    }
}
