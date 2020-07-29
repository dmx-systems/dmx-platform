package systems.dmx.core;



/**
 * A <i>topic</i> at the end of an {@link Assoc}.
 * <p>
 * A <code>TopicPlayer</code> has a {@link Topic} and a role type. The role type expresses the role the Topic
 * plays in the association.
 * <p>
 * The topic (player) is referred to either by ID or URI. The role type is referred to by URI.
 */
public interface TopicPlayer extends Player {

    Topic getTopic();

    String getTopicUri();
}
