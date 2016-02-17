package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationRole;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.model.AssociationRoleModel;



/**
 * An association role that is attached to the {@link DeepaMehtaService}.
 */
class AssociationRoleImpl extends RoleImpl implements AssociationRole {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationRoleImpl(AssociationRoleModel model, Association assoc, EmbeddedService dms) {
        super(model, assoc, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public DeepaMehtaObject getPlayer() {
        return dms.getAssociation(getPlayerId());
    }



    // === AssociationRole Implementation ===

    @Override
    public Association getAssociation() {
        return (Association) getPlayer();
    }



    // === RoleImpl Overrides ===

    @Override
    public AssociationRoleModel getModel() {
        return (AssociationRoleModel) super.getModel();
    }
}
