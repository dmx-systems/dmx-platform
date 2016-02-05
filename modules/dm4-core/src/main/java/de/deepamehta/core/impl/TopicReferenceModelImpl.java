package de.deepamehta.core.impl;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicReferenceModel;

import java.util.List;



class TopicReferenceModelImpl extends RelatedTopicModelImpl implements TopicReferenceModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicReferenceModelImpl(RelatedTopicModel relatedTopic) {
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

    /**
     * Checks weather this reference refers to the given topic.
     */
    @Override
    public boolean isReferingTo(Topic topic) {
        if (isReferenceById()) {
            return getId() == topic.getId();
        } else if (isReferenceByUri()) {
            return getUri().equals(topic.getUri());
        } else {
            throw new RuntimeException("Invalid topic reference (" + this + ")");
        }
    }

    /**
     * From the given topics finds the one this reference refers to.
     */
    @Override
    public RelatedTopic findReferencedTopic(List<RelatedTopic> topics) {
        for (RelatedTopic topic : topics) {
            if (isReferingTo(topic)) {
                return topic;
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
