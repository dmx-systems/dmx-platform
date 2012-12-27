package de.deepamehta.mehtagraph.spi;

import de.deepamehta.mehtagraph.MehtaObjectRole;

import java.util.List;



/**
 * An edge in a Mehtagraph: a connection between 2 {@link MehtaObject}s, each one qualified by a role type.
 *
 * @see  MehtaObjectRole
 */
public interface MehtaEdge extends MehtaObject {

    List<MehtaObjectRole> getMehtaObjects();

    MehtaObjectRole getMehtaObject(long objectId);

    /**
     * Returns the mehta object that plays the given role in this mehta edge.
     * <p>
     * If more than one such mehta object exists, an exception is thrown.
     */
    MehtaObject getMehtaObject(String roleType);
}
