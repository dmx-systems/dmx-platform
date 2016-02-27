package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;



/**
 * Data that underlies a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class TopicTypeModelImpl extends TypeModelImpl implements TopicTypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicTypeModelImpl(TypeModelImpl type) {
        super(type);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public TopicTypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return (TopicTypeModel) super.addAssocDef(assocDef);
    }

    // ---

    @Override
    public String toString() {
        return "topic type (" + super.toString() + ")";
    }
}
