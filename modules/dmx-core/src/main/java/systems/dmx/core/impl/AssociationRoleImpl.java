package systems.dmx.core.impl;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationRole;



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
