package systems.dmx.core;



/**
 * An <i>association</i> at the end of an {@link Assoc}.
 * <p>
 * An <code>AssocPlayer</code> has an {@link Assoc} and a role type. The role type expresses the role the Assoc
 * plays in the association.
 * <p>
 * The assoc (player) is referred to by ID. The role type is referred to by URI.
 */
public interface AssocPlayer extends Player {

    Assoc getAssoc();
}
