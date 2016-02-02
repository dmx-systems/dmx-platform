package de.deepamehta.core.impl;

import de.deepamehta.core.model.TopicDeletionModel;



class TopicDeletionModelImpl extends RelatedTopicModelImpl implements TopicDeletionModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicDeletionModelImpl(long topicId) {
        super(topicId);
    }

    TopicDeletionModelImpl(String topicUri) {
        super(topicUri);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "delete " + super.toString();
    }
}
