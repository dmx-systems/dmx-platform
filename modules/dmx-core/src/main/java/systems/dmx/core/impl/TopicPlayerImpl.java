package systems.dmx.core.impl;

import systems.dmx.core.Topic;
import systems.dmx.core.TopicPlayer;



/**
 * A topic player that is attached to the {@link AccessLayer}.
 */
class TopicPlayerImpl extends PlayerImpl implements TopicPlayer {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicPlayerImpl(TopicPlayerModelImpl model, AssocModelImpl assoc) {
        super(model, assoc);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicPlayer Implementation ===

    @Override
    public Topic getTopic() {
        return (Topic) getDMXObject();
    }

    @Override
    public String getTopicUri() {
        return getModel().getTopicUri();
    }



    // === PlayerImpl Overrides ===

    @Override
    public TopicPlayerModelImpl getModel() {
        return (TopicPlayerModelImpl) model;
    }
}
