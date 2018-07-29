package systems.dmx.core.impl;

import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.service.Directive;

import java.util.List;



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
    public TopicTypeModelImpl clone() {
        try {
            return (TopicTypeModelImpl) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a TopicTypeModel failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "topic type";
    }

    @Override
    TopicTypeImpl instantiate() {
        // Note: declaration and assignment is required for type inference to work (at least in Java 6)
        TopicTypeModelImpl type = clone().filterReadableAssocDefs();
        return new TopicTypeImpl(type, pl);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    List<TopicModelImpl> getAllInstances() {
        return pl.fetchTopics("typeUri", new SimpleValue(uri));
    }

    // ---

    @Override
    Directive getUpdateTypeDirective() {
        return Directive.UPDATE_TOPIC_TYPE;
    }

    @Override
    Directive getDeleteTypeDirective() {
        return Directive.DELETE_TOPIC_TYPE;
    }
}
