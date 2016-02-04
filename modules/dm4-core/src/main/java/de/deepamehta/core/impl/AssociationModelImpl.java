package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;

import org.codehaus.jettison.json.JSONObject;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class AssociationModelImpl extends DeepaMehtaObjectModelImpl implements AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModel roleModel1;   // may be null in models used for an update operation
    private RoleModel roleModel2;   // may be null in models used for an update operation

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationModelImpl(DeepaMehtaObjectModel object, RoleModel roleModel1, RoleModel roleModel2) {
        super(object);
        this.roleModel1 = roleModel1;
        this.roleModel2 = roleModel2;
    }

    /* AssociationModelImpl(AssociationModel assoc) {
        super(assoc);
        this.roleModel1 = assoc.getRoleModel1();
        this.roleModel2 = assoc.getRoleModel2();
    } */

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public RoleModel getRoleModel1() {
        return roleModel1;
    }

    @Override
    public RoleModel getRoleModel2() {
        return roleModel2;
    }

    // ---

    @Override
    public void setRoleModel1(RoleModel roleModel1) {
        this.roleModel1 = roleModel1;
    }

    @Override
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
    @Override
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

    @Override
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

    @Override
    public boolean hasSameRoleTypeUris() {
        return roleModel1.getRoleTypeUri().equals(roleModel2.getRoleTypeUri());
    }



    // === Implementation of the abstract methods ===

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        return new AssociationRoleModel(getId(), roleTypeUri);
    }



    // === Serialization ===

    @Override
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
            model.setRoleModel1(roleModel1.clone());
            model.setRoleModel2(roleModel2.clone());
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Cloning an AssociationModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "association (" + super.toString() + ", " + roleModel1 + ", " + roleModel2 + ")";
    }
}
