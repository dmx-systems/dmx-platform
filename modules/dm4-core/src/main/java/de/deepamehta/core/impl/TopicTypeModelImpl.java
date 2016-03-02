package de.deepamehta.core.impl;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.Directive;



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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "topic type";
    }

    @Override
    TopicType instantiate() {
        return new TopicTypeImpl(this, pl);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    void putInTypeCache() {
        pl.typeCache.putTopicType(instantiate());
    }

    @Override
    void removeFromTypeCache() {
        pl.typeCache.removeTopicType(uri);
    }

    // ---

    @Override
    Directive getDeleteTypeDirective() {
        return Directive.DELETE_TOPIC_TYPE;
    }
}
