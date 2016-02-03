package de.deepamehta.core.model;



/**
 * The role a topic plays in an association.
 * <p>
 * A TopicRoleModel object is a pair of a topic reference and a role type reference.
 * The topic is refered to either by its ID or URI.
 * The role type is refered to by its URI.
 * <p>
 * Assertion: both, the topic reference and the role type reference are set.
 * <p>
 * In the database a role type is represented by a topic of type "dm4.core.role_type".
 */
public interface TopicRoleModel extends RoleModel {

    String getTopicUri();

    boolean topicIdentifiedByUri();
}
