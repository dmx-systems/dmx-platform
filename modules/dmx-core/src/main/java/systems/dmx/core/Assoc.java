package systems.dmx.core;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.TopicPlayerModel;

import java.util.List;



/**
 * An association with 2 ends. At each end can be a {@link DMXObject}, that is either a {@link Topic} or another
 * {@link Assoc}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface Assoc extends DMXObject {

    Player getPlayer1();

    Player getPlayer2();

    // --- Convenience Methods ---

    // TODO: rename to "getPlayerObject1"
    // TODO: return <O extends RelatedObject>
    DMXObject getDMXObject1();

    // TODO: rename to "getPlayerObject2"
    // TODO: return <O extends RelatedObject>
    DMXObject getDMXObject2();

    // ---

    /**
     * TODO: rename method to "getPlayerObjectByRole"
     *
     * @return  this association's player which plays the given role.
     *          If there is no such player, null is returned.   ### FIXDOC
     *          <p>
     *          If there are 2 such players an exception is thrown.
     */
    <O extends RelatedObject> O getDMXObjectByRole(String roleTypeUri);

    /**
     * TODO: rename method to "getPlayerObjectByType"
     * TODO: return <O extends RelatedObject>
     *
     * @return  this association's topic which has the given type.
     *          If there is no such topic, null is returned.    ### FIXDOC
     *          <p>
     *          If there are 2 such topics an exception is thrown.
     */
    DMXObject getDMXObjectByType(String topicTypeUri);

    // ---

    /**
     * @return  this association's player that plays the given role.
     *          If no player matches, null is returned.
     *          If both players are matching an exception is thrown.
     */
    Player getPlayerByRole(String roleTypeUri);

    int playerCount(String roleTypeUri);

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

    @Override
    AssocModel getModel();
}
