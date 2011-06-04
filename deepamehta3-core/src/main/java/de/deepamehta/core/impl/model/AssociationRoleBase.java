package de.deepamehta.core.impl.model;

import de.deepamehta.core.AssociationRole;
import de.deepamehta.core.model.AssociationRoleModel;

import org.codehaus.jettison.json.JSONObject;



public class AssociationRoleBase extends RoleBase implements AssociationRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected AssociationRoleBase(AssociationRoleModel model) {
        super(model);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssociationRole Implementation ===

    @Override
    public long getAssociationId() {
        return getModel().getAssociationId();
    }



    // === Role Implementation ===

    @Override
    public JSONObject toJSON() {
        return getModel().toJSON();
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods



    // === RoleBase Overrides ===

    @Override
    protected AssociationRoleModel getModel() {
        return (AssociationRoleModel) super.getModel();
    }
}
