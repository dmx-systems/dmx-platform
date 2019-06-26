package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedTopic;



/**
 * A Topic-Assoc pair that is attached to the {@link AccessLayer}.
 */
class RelatedTopicImpl extends TopicImpl implements RelatedTopic {

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicImpl(RelatedTopicModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Assoc getRelatingAssoc() {
        return getModel().getRelatingAssoc().instantiate();
    }

    @Override
    public RelatedTopicModelImpl getModel() {
        return (RelatedTopicModelImpl) model;
    }
}
