package de.deepamehta.core.impl;

import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
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

    @Override
    public boolean isEmptyRef() {
        return !isReferenceById() && !isReferenceByUri();
    }

    // ---

    @Override
    public boolean isReferingTo(TopicModel topic) {
        if (isReferenceById()) {
            return getId() == topic.getId();
        } else if (isReferenceByUri()) {
            return getUri().equals(topic.getUri());
        } else {
            throw new RuntimeException("Invalid " + this);
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "topic reference";
    }

    // ---

    TopicModelImpl resolve() {
        try {
            TopicModelImpl topic;
            // Note: the resolved topic must be fetched including its child topics.
            // They might be required for label calculation and/or at client-side. ### TODO?
            if (isReferenceById()) {
                topic = pl.fetchTopic(id);    // .loadChildTopics();  // TODO?
            } else if (isReferenceByUri()) {
                topic = pl.fetchTopic("uri", new SimpleValue(uri));
                if (topic == null) {
                    throw new RuntimeException("Topic with URI \"" + uri + "\" not found");
                }
                // .loadChildTopics();  // TODO?
            } else {
                throw new RuntimeException("Invalid " + this);
            }
            // TODO: why is set() required?
            // Without it the custom assoc type refs in the type cache are unresolved (ID=-1) after bootstrap
            this.set(topic);
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Resolving a topic reference failed (" + this + ")", e);
        }
    }
}
