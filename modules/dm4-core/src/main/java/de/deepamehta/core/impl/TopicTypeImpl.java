package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;

import java.util.List;
import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class TopicTypeImpl extends TypeImpl implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicTypeImpl(TopicTypeModel model, PersistenceLayer pl) {
        super(model, pl);
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



    // === TopicImpl Overrides ===

    @Override
    final String className() {
        return "topic type";
    }



    // === Implementation of abstract TypeImpl methods ===

    @Override
    final void putInTypeCache() {
        pl.typeCache.putTopicType(this);
    }

    @Override
    final void removeFromTypeCache() {
        pl.typeCache.removeTopicType(getUri());
    }

    // ---

    @Override
    final Directive getDeleteTypeDirective() {
        return Directive.DELETE_TOPIC_TYPE;
    }

    @Override
    final List<? extends DeepaMehtaObject> getAllInstances() {
        return pl.getTopics(getUri()).getItems();
    }
}
