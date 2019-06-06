package systems.dmx.core.model;



/**
 * Data that underlies a {@link AssocType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssocTypeModel extends TypeModel {

    @Override
    AssocTypeModel addCompDef(CompDefModel compDef);
}
