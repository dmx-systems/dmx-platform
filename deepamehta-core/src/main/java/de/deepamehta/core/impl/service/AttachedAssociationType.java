package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationTypeModel;



/**
 * An association type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationType extends AttachedType implements AssociationType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationType(EmbeddedService dms) {
        super(dms);     // The model remains uninitialized.
                        // It is initialized later on through fetch().
    }

    AttachedAssociationType(AssociationTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods



    // *******************************
    // *** AttachedTopic Overrides ***
    // *******************************



    @Override
    protected String className() {
        return "association type";
    }
}
