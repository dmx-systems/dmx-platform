package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationRole;
import de.deepamehta.core.DeepaMehtaObject;
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



    // === Role Implementation ===

    @Override
    public DeepaMehtaObject getPlayer() {
        return dms.getAssociation(getPlayerId(), false);    // fetchComposite=false
    }



    // === AssociationRole Implementation ===

    @Override
    public Association getAssociation() {
        return (Association) getPlayer();
    }



    // === AttachedRole Overrides ===

    @Override
    public AssociationRoleModel getModel() {
        return (AssociationRoleModel) super.getModel();
    }
}
