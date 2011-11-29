package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * The role an association plays in an association.
 * <p>
 * A AssociationRoleModel object is a pair of an association ID and a role type URI.
 * <p>
 * Assertion: both, the association ID and the role type URI are set.
 * <p>
 * In the database a role type is represented by a topic of type "dm4.core.role_type".
 */
public class AssociationRoleModel extends RoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long assocId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationRoleModel(long assocId, String roleTypeUri) {
        super(roleTypeUri);
        this.assocId = assocId;
    }

    public AssociationRoleModel(JSONObject assocRoleModel) {
        try {
            this.assocId = assocRoleModel.getLong("assoc_id");
            this.roleTypeUri = assocRoleModel.getString("role_type_uri");
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationRoleModel failed (JSONObject=" + assocRoleModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getAssociationId() {
        return assocId;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("assoc_id", assocId);
            o.put("role_type_uri", roleTypeUri);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n        association role (roleTypeUri=\"" + roleTypeUri + "\", assocId=" + assocId + ")";
    }
}
