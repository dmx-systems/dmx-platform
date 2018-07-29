package systems.dmx.core.impl;

import systems.dmx.core.TopicType;
import systems.dmx.core.model.TopicTypeModel;



/**
 * A topic type that is attached to the {@link CoreService}.
 */
class TopicTypeImpl extends DMXTypeImpl implements TopicType {

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
    public void update(TopicTypeModel updateModel) {
        model.update((TopicTypeModelImpl) updateModel);     // ### FIXME: call through pl for access control
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    TopicTypeModelImpl _getModel() {
        return pl._getTopicType(getUri());
    }
}
