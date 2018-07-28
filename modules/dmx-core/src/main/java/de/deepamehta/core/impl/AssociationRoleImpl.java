package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationRole;



/**
 * An association role that is attached to the {@link PersistenceLayer}.
 */
class AssociationRoleImpl extends RoleImpl implements AssociationRole {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationRoleImpl(AssociationRoleModelImpl model, AssociationModelImpl assoc) {
        super(model, assoc);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssociationRole Implementation ===

    @Override
    public Association getAssociation() {
        return (Association) getPlayer();
    }



    // === RoleImpl Overrides ===

    @Override
    public AssociationRoleModelImpl getModel() {
        return (AssociationRoleModelImpl) model;
    }
}
