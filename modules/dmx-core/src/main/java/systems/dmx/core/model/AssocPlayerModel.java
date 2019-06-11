package systems.dmx.core.model;



/**
 * An association-end of an association.
 * <p>
 * A AssocPlayerModel object is a pair of an association ID and a role type URI.
 * <p>
 * Assertion: both, the association ID and the role type URI are set.
 * <p>
 * In the database a role type is represented by a topic of type "dmx.core.role_type".
 */
public interface AssocPlayerModel extends PlayerModel {
}
