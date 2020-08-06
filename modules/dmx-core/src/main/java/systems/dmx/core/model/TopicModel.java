package systems.dmx.core.model;



/**
 * The data that underly a {@link Topic}.
 * <p>
 * A <code>TopicModel</code> can also be used to provide the data for a topic <i>create</i> or <i>update</i>
 * operation. To instantiate a <code>TopicModel</code> use the {@link ModelFactory}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface TopicModel extends DMXObjectModel {

    TopicModel clone();
}
