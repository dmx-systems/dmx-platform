package de.deepamehta.core.model;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationModel extends DMXObjectModel {

    RoleModel getRoleModel1();

    RoleModel getRoleModel2();

    // ---

    void setRoleModel1(RoleModel roleModel1);

    void setRoleModel2(RoleModel roleModel2);

    // --- Convenience Methods ---

    /**
     * @return  this association's role that matches the given role type.
     *          If no role matches, null is returned.
     *          If both roles are matching an exception is thrown.
     */
    RoleModel getRoleModel(String roleTypeUri);

    boolean hasSameRoleTypeUris();

    /**
     * Checks if the given players match this association.
     * The given role type URIs must be different.
     * The player position ("1" vs. "2") is not relevant.
     *
     * @return  true if the given players match this association.
     *
     * @throws  IllegalArgumentException    if both given role type URIs are identical.
     */
    boolean matches(String roleTypeUri1, long playerId1, String roleTypeUri2, long playerId2);

    long getOtherPlayerId(long id);

    // ---

    AssociationModel clone();
}
