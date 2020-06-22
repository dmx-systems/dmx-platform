package systems.dmx.core.impl;

import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicReferenceModel;

import java.util.List;



class TopicReferenceModelImpl extends RelatedTopicModelImpl implements TopicReferenceModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    long originalId = -1;

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

    /**
     * Resolves this reference and replaces it by the resolved topic.
     *
     * TODO: don't manipulate in-place?
     *
     * @throws  RuntimeException    if retrieval of the refered topic fails
     * @throws  RuntimeException    if this reference refers to nothing
     */
    TopicModelImpl resolve() {
        try {
            TopicModelImpl topic;
            // Note: the resolved topic must be fetched including its child topics.
            // They might be required for label calculation and/or at client-side. ### TODO?
            if (isReferenceById()) {
                topic = al.db.fetchTopic(id);    // .loadChildTopics();  // TODO?
            } else if (isReferenceByUri()) {
                topic = al.fetchTopic("uri", uri);
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
