package systems.dmx.core.model;



/**
 * The data that underly a {@link TopicPlayer}.
 */
public interface TopicPlayerModel extends PlayerModel {

    String getTopicUri();

    boolean topicIdentifiedByUri();
}
