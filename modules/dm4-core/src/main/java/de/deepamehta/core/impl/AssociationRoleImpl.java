package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationRole;
import de.deepamehta.core.DeepaMehtaObject;



/**
 * An association role that is attached to the {@link PersistenceLayer}.
 */
class AssociationRoleImpl extends RoleImpl implements AssociationRole {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationRoleImpl(AssociationRoleModelImpl model, AssociationModelImpl assoc, PersistenceLayer pl) {
        super(model, assoc, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public DeepaMehtaObject getPlayer() {
        return new AssociationImpl(getModel().getPlayer(), pl);     // ### TODO: permission check?
    }



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
