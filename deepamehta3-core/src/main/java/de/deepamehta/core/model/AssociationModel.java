package de.deepamehta.core.model;

import de.deepamehta.core.Association;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationModel extends DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModel roleModel1;
    private RoleModel roleModel2;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationModel(String typeUri) {
        super(typeUri);
    }

    public AssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        this(-1, typeUri, roleModel1, roleModel2);
    }

    public AssociationModel(long id, String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        super(id, typeUri);
        this.roleModel1 = roleModel1;
        this.roleModel2 = roleModel2;
    }

    public AssociationModel(JSONObject assocModel) {
        super(assocModel);
        try {
            this.roleModel1 = parseRole(assocModel.getJSONObject("role_1"));
            this.roleModel2 = parseRole(assocModel.getJSONObject("role_2"));
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationModel failed (JSONObject=" + assocModel + ")", e);
        }
    }

    // ---

    protected AssociationModel() {
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public RoleModel getRoleModel1() {
        return roleModel1;
    }

    public RoleModel getRoleModel2() {
        return roleModel2;
    }

    // ---

    public void setRoleModel1(RoleModel roleModel) {
        this.roleModel1 = roleModel;
    }

    public void setRoleModel2(RoleModel roleModel) {
        this.roleModel2 = roleModel;
    }



    // === Serialization ===

    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("role_1", roleModel1.toJSON());
            o.put("role_2", roleModel2.toJSON());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public String toString() {
        return "association model (id=" + id + ", uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", composite=" + composite + ", roleModel1=" + roleModel1 + ", roleModel2=" + roleModel2 + ")";
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private RoleModel parseRole(JSONObject roleModel) {
        if (roleModel.has("topic_id") || roleModel.has("topic_uri")) {
            return new TopicRoleModel(roleModel);
        } else if (roleModel.has("assoc_id")) {
            return new AssociationRoleModel(roleModel);
        } else {
            throw new RuntimeException("Parsing TopicRoleModel/AssociationRoleModel failed " +
                "(JSONObject=" + roleModel + ")");
        }
    }
}
