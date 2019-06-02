package systems.dmx.core.model;



/**
 * Data that underlies a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface TopicTypeModel extends TypeModel {

    @Override
    TopicTypeModel addCompDef(CompDefModel compDef);
}
