package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;

import java.util.List;
import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicType extends AttachedType implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** TopicType Implementation ***
    // ********************************



    @Override
    public TopicTypeModelImpl getModel() {
        return (TopicTypeModelImpl) super.getModel();
    }

    @Override
    public void update(TopicTypeModel model) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + model + ")");
        // Note: the UPDATE_TOPIC_TYPE directive must be added *before* a possible UPDATE_TOPIC directive (added
        // by super.update()). In case of a changed type URI the webclient's type cache must be updated *before*
        // the TopicTypeRenderer can render the type.
        Directives.get().add(Directive.UPDATE_TOPIC_TYPE, this);
        //
        super.update(model);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === AttachedTopic Overrides ===

    @Override
    final String className() {
        return "topic type";
    }



    // === Implementation of abstract AttachedType methods ===

    @Override
    final void putInTypeCache() {
        dms.typeCache.putTopicType(this);
    }

    @Override
    final void removeFromTypeCache() {
        dms.typeCache.removeTopicType(getUri());
    }

    // ---

    @Override
    final Directive getDeleteTypeDirective() {
        return Directive.DELETE_TOPIC_TYPE;
    }

    @Override
    final List<? extends DeepaMehtaObject> getAllInstances() {
        return dms.getTopics(getUri()).getItems();
    }
}
