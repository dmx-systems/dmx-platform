package de.deepamehta.core.impl;

import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;

import java.util.List;



class TopicReferenceModelImpl extends RelatedTopicModelImpl implements TopicReferenceModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicReferenceModelImpl(RelatedTopicModelImpl relatedTopic) {
        super(relatedTopic);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public boolean isReferenceById() {
        return getId() != -1;
    }

    @Override
    public boolean isReferenceByUri() {
        return getUri() != null && !getUri().equals("");
    }

    // ---

    @Override
    public boolean isReferingTo(TopicModel topic) {
        if (isReferenceById()) {
            return getId() == topic.getId();
        } else if (isReferenceByUri()) {
            return getUri().equals(topic.getUri());
        } else {
            throw new RuntimeException("Invalid topic reference (" + this + ")");
        }
    }

    @Override
    public RelatedTopicModelImpl findReferencedTopic(List<? extends RelatedTopicModel> topics) {
        for (RelatedTopicModel topic : topics) {
            if (isReferingTo(topic)) {
                return (RelatedTopicModelImpl) topic;
            }
        }
        return null;
    }

    // ---

    @Override
    public String toString() {
        return "reference " + super.toString();
    }
}
