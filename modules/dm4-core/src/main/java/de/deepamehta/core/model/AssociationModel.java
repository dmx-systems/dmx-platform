package de.deepamehta.core.model;

import de.deepamehta.core.Association;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

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

    private RoleModel roleModel1;   // may be null in models used for an update operation
    private RoleModel roleModel2;   // may be null in models used for an update operation

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        this(typeUri, roleModel1, roleModel2, null);
    }

    public AssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2, CompositeValueModel composite) {
        this(-1, null, typeUri, roleModel1, roleModel2, null, composite);
    }

    public AssociationModel(long id) {
        super(id);
    }

    public AssociationModel(long id, String uri, String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        this(id, uri, typeUri, roleModel1, roleModel2, null, null);
    }

    public AssociationModel(long id, String uri, String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                     SimpleValue value, CompositeValueModel composite) {
        super(id, uri, typeUri, value, composite);
        this.roleModel1 = roleModel1;
        this.roleModel2 = roleModel2;
    }

    public AssociationModel(AssociationModel model) {
        this(model.getId(), model.getUri(), model.getTypeUri(), model.getRoleModel1(), model.getRoleModel2());
    }

    public AssociationModel(JSONObject assocModel) {
        super(assocModel);
        try {
            if (assocModel.has("role_1")) {
                this.roleModel1 = parseRole(assocModel.getJSONObject("role_1"));
            }
            if (assocModel.has("role_2")) {
                this.roleModel2 = parseRole(assocModel.getJSONObject("role_2"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationModel failed (JSONObject=" + assocModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public RoleModel getRoleModel1() {
        return roleModel1;
    }

    public RoleModel getRoleModel2() {
        return roleModel2;
    }

    // --- Convenience Methods ---

    public RoleModel getRoleModel(String roleTypeUri) {
        boolean rm1 = roleModel1.getRoleTypeUri().equals(roleTypeUri);
        boolean rm2 = roleModel2.getRoleTypeUri().equals(roleTypeUri);
        if (rm1 && rm2) {
            throw new RuntimeException("Ambiguous getRoleModel() call: both players occupy role \"" +
                roleTypeUri + "\" in association (" + this + ")");
        } else if (rm1) {
            return roleModel1;
        } else if (rm2) {
            return roleModel2;
        }
        return null;
    }

    public long getOtherPlayerId(long id) {
        long id1 = roleModel1.getPlayerId();
        long id2 = roleModel2.getPlayerId();
        if (id1 == id) {
            return id2;
        } else if (id2 == id) {
            return id1;
        } else {
            throw new IllegalArgumentException("ID " + id + " doesn't refer to a player in " + this);
        }
    }

    public boolean hasSameRoleTypeUris() {
        return roleModel1.getRoleTypeUri().equals(roleModel2.getRoleTypeUri());
    }



    // === Implementation of the abstract methods ===

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        return new AssociationRoleModel(getId(), roleTypeUri);
    }



    // === Serialization ===

    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("role_1", roleModel1.toJSON());
            o.put("role_2", roleModel2.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public AssociationModel clone() {
        try {
            AssociationModel model = (AssociationModel) super.clone();
            model.roleModel1 = roleModel1.clone();
            model.roleModel2 = roleModel2.clone();
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Cloning an AssociationModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "association (" + super.toString() + "," + roleModel1 + "," + roleModel2 + ")";
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
