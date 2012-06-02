package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationRole;
import de.deepamehta.core.model.AssociationRoleModel;



/**
 * An association role that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationRole extends AttachedRole implements AssociationRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationRole(AssociationRoleModel model, Association assoc, EmbeddedService dms) {
        super(model, assoc, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssociationRole Implementation ===

    @Override
    public long getAssociationId() {
        return getModel().getAssociationId();
    }

    // === AttachedRole Overrides ===

    @Override
    public AssociationRoleModel getModel() {
        return (AssociationRoleModel) super.getModel();
    }
}
