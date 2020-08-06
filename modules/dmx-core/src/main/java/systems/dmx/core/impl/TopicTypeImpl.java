package systems.dmx.core.impl;

import systems.dmx.core.TopicType;
import systems.dmx.core.model.TopicTypeModel;



/**
 * A topic type that is attached to the {@link CoreService}.
 */
class TopicTypeImpl extends DMXTypeImpl implements TopicType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicTypeImpl(TopicTypeModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *****************
    // *** TopicType ***
    // *****************



    @Override
    public TopicTypeModelImpl getModel() {
        return (TopicTypeModelImpl) model;
    }

    @Override
    public void update(TopicTypeModel updateModel) {
        model.update((TopicTypeModelImpl) updateModel);     // ### FIXME: call through al for access control
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    TopicTypeModelImpl _getModel() {
        return al._getTopicType(getUri());
    }
}
