package systems.dmx.core.impl;

import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicDeletionModel;



class TopicDeletionModelImpl extends RelatedTopicModelImpl implements TopicDeletionModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicDeletionModelImpl(RelatedTopicModelImpl relatedTopic) {
        super(relatedTopic);
    }
}
