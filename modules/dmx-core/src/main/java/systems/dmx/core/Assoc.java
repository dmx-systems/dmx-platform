package systems.dmx.core;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.TopicPlayerModel;

import java.util.List;



/**
 * ### FIXDOC: Specification of an association -- A n-ary connection between topics and other associations.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Assoc extends DMXObject {

    Player getPlayer1();

    Player getPlayer2();

    // --- Convenience Methods ---

    DMXObject getDMXObject1();

    DMXObject getDMXObject2();

    // ---

    /**
     * @return  this association's player which plays the given role.
     *          If there is no such player, null is returned.
     *          <p>
     *          If there are 2 such players an exception is thrown.
     */
    RelatedObject getDMXObjectByRole(String roleTypeUri);

    /**
     * TODO: return RelatedObject
     *
     * @return  this association's topic which has the given type.
     *          If there is no such topic, null is returned.
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

    /**
     * ### TODO: rethink this method
     *
     * Returns this association's role which refers to the same object as the given role model.
     * The role returned is found by comparing topic IDs, topic URIs, or association IDs.
     * The role types are <i>not</i> compared.
     * <p>
     * If the object refered by the given role model is not a player in this association an exception is thrown.
     */
    Player getRole(PlayerModel roleModel);

    /**
     * ### TODO: drop it
     */
    boolean isPlayer(TopicPlayerModel roleModel);

    // ---

    void update(AssocModel model);

    // ---

    @Override
    Assoc loadChildTopics();

    @Override
    Assoc loadChildTopics(String compDefUri);

    // ---

    @Override
    AssocModel getModel();
}
