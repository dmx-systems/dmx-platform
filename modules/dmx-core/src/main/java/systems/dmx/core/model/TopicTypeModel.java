package systems.dmx.core.model;



/**
 * The data that underly a {@link TopicType}.
 * <p>
 * A <code>TopicTypeModel</code> can also be used to provide the data for a topic type <i>create</i> or <i>update</i>
 * operation. To instantiate a <code>TopicTypeModel</code> use the {@link ModelFactory}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface TopicTypeModel extends TypeModel {

    @Override
    TopicTypeModel addCompDef(CompDefModel compDef);
}
