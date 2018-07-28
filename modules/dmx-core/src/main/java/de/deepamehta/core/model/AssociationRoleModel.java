package de.deepamehta.core.model;



/**
 * The role an association plays in an association.
 * <p>
 * A AssociationRoleModel object is a pair of an association ID and a role type URI.
 * <p>
 * Assertion: both, the association ID and the role type URI are set.
 * <p>
 * In the database a role type is represented by a topic of type "dm4.core.role_type".
 */
public interface AssociationRoleModel extends RoleModel {
}
