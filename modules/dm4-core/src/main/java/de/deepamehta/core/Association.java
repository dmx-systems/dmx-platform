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



    // === Model ===

    Role getRole1();

    Role getRole2();

    // ---

    DeepaMehtaObject getPlayer1();

    DeepaMehtaObject getPlayer2();

    // ---

    /**
     * @teturn  this association's topic which plays the given role.
     *          If there is no such topic, null is returned.
     *          <p>
     *          If there are 2 such topics an exception is thrown.
     */
    Topic getTopic(String roleTypeUri);

    /**
     * @teturn  this association's topic which has the given type.
     *          If there is no such topic, null is returned.
     *          <p>
     *          If there are 2 such topics an exception is thrown.
     */
    Topic getTopicByType(String topicTypeUri);

    // ---

    /**
     * Returns this association's role which refers to the same object as the given role model.
     * The role returned is found by comparing topic IDs, topic URIs, or association IDs.
     * The role types are <i>not</i> compared.
     * <p>
     * If the object refered by the given role model is not a player in this association an exception is thrown.
     */
    Role getRole(RoleModel roleModel);

    boolean isPlayer(TopicRoleModel roleModel);

    // ---

    Association loadChildTopics();
    Association loadChildTopics(String childTypeUri);

    // ---

    AssociationModel getModel();



    // === Updating ===

    void update(AssociationModel model);
}
