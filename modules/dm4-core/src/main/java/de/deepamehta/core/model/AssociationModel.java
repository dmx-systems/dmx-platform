package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



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

    public AssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2, ChildTopicsModel childTopics) {
        this(-1, null, typeUri, roleModel1, roleModel2, null, childTopics);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    public AssociationModel() {
        super(-1);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    public AssociationModel(ChildTopicsModel childTopics) {
        super(childTopics);
    }

    public AssociationModel(long id, String uri, String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        this(id, uri, typeUri, roleModel1, roleModel2, null, null);
    }

    public AssociationModel(long id, String uri, String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                 SimpleValue value, ChildTopicsModel childTopics) {
        super(id, uri, typeUri, value, childTopics);
        this.roleModel1 = roleModel1;
        this.roleModel2 = roleModel2;
    }

    public AssociationModel(AssociationModel assoc) {
        super(assoc);
        this.roleModel1 = assoc.getRoleModel1();
        this.roleModel2 = assoc.getRoleModel2();
    }

    public AssociationModel(JSONObject assoc) {
        super(assoc);
        try {
            if (assoc.has("role_1")) {
                this.roleModel1 = parseRole(assoc.getJSONObject("role_1"));
            }
            if (assoc.has("role_2")) {
                this.roleModel2 = parseRole(assoc.getJSONObject("role_2"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationModel failed (JSONObject=" + assoc + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public RoleModel getRoleModel1() {
        return roleModel1;
    }

    public RoleModel getRoleModel2() {
        return roleModel2;
    }

    // ---

    public void setRoleModel1(RoleModel roleModel1) {
        this.roleModel1 = roleModel1;
    }

    public void setRoleModel2(RoleModel roleModel2) {
        this.roleModel2 = roleModel2;
    }

    // --- Convenience Methods ---

    /**
     * @teturn  this association's role that matches the given role type.
     *          If no role matches, null is returned.
     *          <p>
     *          If both roles are matching an exception is thrown.
     */
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
        return "association (" + super.toString() + ", " + roleModel1 + ", " + roleModel2 + ")";
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
