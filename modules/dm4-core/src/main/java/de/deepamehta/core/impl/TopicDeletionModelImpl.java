package de.deepamehta.core.impl;

import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicDeletionModel;



class TopicDeletionModelImpl extends RelatedTopicModelImpl implements TopicDeletionModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicDeletionModelImpl(RelatedTopicModelImpl relatedTopic) {
        super(relatedTopic);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "delete " + super.toString();
    }
}
