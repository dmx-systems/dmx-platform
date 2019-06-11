package systems.dmx.core.model;



/**
 * A topic-end of an association.
 * <p>
 * A TopicPlayerModel object is a pair of a topic reference and a role type reference.
 * The topic is refered to either by its ID or URI.
 * The role type is refered to by its URI.
 * <p>
 * Assertion: both, the topic reference and the role type reference are set.
 * <p>
 * In the database a role type is represented by a topic of type "dmx.core.role_type".
 */
public interface TopicPlayerModel extends PlayerModel {

    String getTopicUri();

    boolean topicIdentifiedByUri();
}
