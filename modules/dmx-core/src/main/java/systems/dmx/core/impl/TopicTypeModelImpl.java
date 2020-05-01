package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicTypeModel;
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
        if (type.dataTypeUri.equals(COMPOSITE)) {
            throw new IllegalArgumentException("\"" + type.dataTypeUri + "\" is an illegal data type for a topic " +
                "type. Use \"dmx.core.value\" or \"dmx.core.entity\" instead. " + type);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public TopicTypeModel addCompDef(CompDefModel compDef) {
        return (TopicTypeModel) super.addCompDef(compDef);
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
        return new TopicTypeImpl(clone().filterReadableCompDefs(), al);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    List<TopicModelImpl> getAllInstances() {
        return al.db.fetchTopics("typeUri", uri);
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
