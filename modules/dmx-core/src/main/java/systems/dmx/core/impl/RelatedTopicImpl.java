package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedTopic;



/**
 * A Topic-Assoc pair that is attached to the {@link PersistenceLayer}.
 */
class RelatedTopicImpl extends TopicImpl implements RelatedTopic {

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicImpl(RelatedTopicModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Assoc getRelatingAssociation() {
        return getModel().getRelatingAssociation().instantiate();
    }

    @Override
    public RelatedTopicModelImpl getModel() {
        return (RelatedTopicModelImpl) model;
    }
}
