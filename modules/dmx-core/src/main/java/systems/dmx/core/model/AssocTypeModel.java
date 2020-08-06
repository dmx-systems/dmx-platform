package systems.dmx.core.model;



/**
 * The data that underly an {@link AssocType}.
 * <p>
 * A <code>AssocTypeModel</code> can also be used to provide the data for an association type <i>create</i> or
 * <i>update</i> operation. To instantiate an <code>AssocTypeModel</code> use the {@link ModelFactory}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface AssocTypeModel extends TypeModel {

    @Override
    AssocTypeModel addCompDef(CompDefModel compDef);
}
