package systems.dmx.core.model;



/**
 * The data that underly an {@link Assoc}.
 * <p>
 * An <code>AssocModel</code> can also be used to provide the data for an association <i>create</i> or <i>update</i>
 * operation. To instantiate an <code>AssocModel</code> use the {@link ModelFactory}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface AssocModel extends DMXObjectModel {

    PlayerModel getPlayer1();

    PlayerModel getPlayer2();

    // ---

    void setPlayer1(PlayerModel player1);

    void setPlayer2(PlayerModel player2);

    // --- Convenience Methods ---

    /**
     * @return  this association's player that plays the given role.
     *          If no player matches, null is returned.
     *          If both players are matching an exception is thrown.
     */
    PlayerModel getPlayerByRole(String roleTypeUri);

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

    AssocModel clone();
}
