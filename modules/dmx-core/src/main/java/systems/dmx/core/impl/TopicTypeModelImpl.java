package systems.dmx.core.impl;

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
        if (type.dataTypeUri.equals("dmx.core.composite")) {
            throw new IllegalArgumentException("\"" + type.dataTypeUri + "\" is an illegal data type for a topic " +
                "type. Use \"dmx.core.value\" or \"dmx.core.identity\" instead. " + type);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public TopicTypeModel addCompDef(CompDefModel assocDef) {
        return (TopicTypeModel) super.addCompDef(assocDef);
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
        TopicTypeModelImpl type = clone().filterReadableCompDefs();
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
