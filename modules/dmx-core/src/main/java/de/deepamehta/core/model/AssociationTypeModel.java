package de.deepamehta.core.model;



/**
 * Data that underlies a {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationTypeModel extends TypeModel {

    @Override
    AssociationTypeModel addAssocDef(AssociationDefinitionModel assocDef);
}
