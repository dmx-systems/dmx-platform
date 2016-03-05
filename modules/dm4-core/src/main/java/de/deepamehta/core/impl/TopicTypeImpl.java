package de.deepamehta.core.impl;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicTypeModel;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class TopicTypeImpl extends TypeImpl implements TopicType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicTypeImpl(TopicTypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** TopicType Implementation ***
    // ********************************



    @Override
    public TopicTypeModelImpl getModel() {
        return (TopicTypeModelImpl) model;
    }

    @Override
    public void update(TopicTypeModel newModel) {
        model.update(newModel);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === TopicImpl Overrides ===

    @Override
    final String className() {
        return "topic type";
    }
}
