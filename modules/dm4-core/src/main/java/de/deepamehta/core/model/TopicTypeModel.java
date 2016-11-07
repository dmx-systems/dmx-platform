package de.deepamehta.core.model;

import java.util.List;



/**
 * Data that underlies a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface TopicTypeModel extends TypeModel {

    @Override
    TopicTypeModel addAssocDef(AssociationDefinitionModel assocDef);

    @Override
    TopicTypeModel setLabelConfig(List<String> labelConfig);
}
