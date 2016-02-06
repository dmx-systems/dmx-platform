package de.deepamehta.core.model;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationModel extends DeepaMehtaObjectModel {

    RoleModel getRoleModel1();

    RoleModel getRoleModel2();

    // ---

    void setRoleModel1(RoleModel roleModel1);

    void setRoleModel2(RoleModel roleModel2);

    // --- Convenience Methods ---

    /**
     * @teturn  this association's role that matches the given role type.
     *          If no role matches, null is returned.
     *          <p>
     *          If both roles are matching an exception is thrown.
     */
    RoleModel getRoleModel(String roleTypeUri);

    long getOtherPlayerId(long id);

    boolean hasSameRoleTypeUris();

    // ---

    AssociationModel clone();
}
