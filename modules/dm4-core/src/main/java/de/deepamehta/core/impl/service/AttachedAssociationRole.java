package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationRole;
import de.deepamehta.core.model.AssociationRoleModel;



/**
 * An association role that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationRole extends AttachedRole implements AssociationRole {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationRole(AssociationRoleModel model, Association assoc, EmbeddedService dms) {
        super(model, assoc, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of the abstract methods ===

    @Override
    void storeRoleTypeUri() {
        dms.storage.storeRoleTypeUri(getAssociation().getId(), getPlayerId(), getRoleTypeUri());
    }



    // === AttachedRole Overrides ===

    @Override
    public AssociationRoleModel getModel() {
        return (AssociationRoleModel) super.getModel();
    }
}
