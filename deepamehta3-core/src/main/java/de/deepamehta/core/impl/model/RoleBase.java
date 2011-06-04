package de.deepamehta.core.impl.model;

import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



public abstract class RoleBase implements Role {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModel model;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected RoleBase(RoleModel model) {
        this.model = model;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public String getRoleTypeUri() {
        return model.getRoleTypeUri();
    }

    @Override
    public void setRoleTypeUri(String roleTypeUri) {
        model.setRoleTypeUri(roleTypeUri);
    }

    // ---

    // Note: toJSON() remains abstract



    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected RoleModel getModel() {
        return model;
    }
}
