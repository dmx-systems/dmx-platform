package de.deepamehta.core.impl;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicTypeModel;



/**
 * A topic type that is attached to the {@link CoreService}.
 */
class TopicTypeImpl extends DeepaMehtaTypeImpl implements TopicType {

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
        model.update(newModel);     // ### FIXME: call through pl for access control
    }
}
