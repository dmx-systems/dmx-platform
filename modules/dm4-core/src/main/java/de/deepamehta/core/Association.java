package de.deepamehta.core;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;

import java.util.List;



/**
 * ### FIXDOC: Specification of an association -- A n-ary connection between topics and other associations.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Association extends DeepaMehtaObject {

    Role getRole1();

    Role getRole2();

    // ---

    DeepaMehtaObject getPlayer1();

    DeepaMehtaObject getPlayer2();

    // --- Convenience Methods ---

    /**
     * @return  this association's role that matches the given role type.
     *          If no role matches, null is returned.
     *          If both roles are matching an exception is thrown.
     */
    Role getRole(String roleTypeUri);

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
     * ### TODO: make it work for assoc players as well or drop it
     *
     * @teturn  this association's topic which plays the given role.
     *          If there is no such topic, null is returned.
     *          <p>
     *          If there are 2 such topics an exception is thrown.
     */
    Topic getTopic(String roleTypeUri);

    /**
     * ### TODO: make it work for assoc players as well or drop it
     *
     * @teturn  this association's topic which has the given type.
     *          If there is no such topic, null is returned.
     *          <p>
     *          If there are 2 such topics an exception is thrown.
     */
    Topic getTopicByType(String topicTypeUri);

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
    Role getRole(RoleModel roleModel);

    /**
     * ### TODO: drop it
     */
    boolean isPlayer(TopicRoleModel roleModel);

    // ---

    void update(AssociationModel model);

    // ---

    Association loadChildTopics();
    Association loadChildTopics(String assocDefUri);

    // ---

    AssociationModel getModel();
}
