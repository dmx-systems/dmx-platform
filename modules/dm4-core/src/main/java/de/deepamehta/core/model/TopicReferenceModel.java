package de.deepamehta.core.model;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import java.util.List;



public class TopicReferenceModel extends RelatedTopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicReferenceModel(long topicId) {
        super(topicId);
    }

    public TopicReferenceModel(long topicId, AssociationModel relatingAssoc) {
        super(topicId, relatingAssoc);
    }

    public TopicReferenceModel(String topicUri) {
        super(topicUri);
    }

    public TopicReferenceModel(String topicUri, AssociationModel relatingAssoc) {
        super(topicUri, relatingAssoc);
    }

    public TopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics) {
        super(topicId, new AssociationModel(relatingAssocChildTopics));
    }

    public TopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics) {
        super(topicUri, new AssociationModel(relatingAssocChildTopics));
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public boolean isReferenceById() {
        return getId() != -1;
    }

    public boolean isReferenceByUri() {
        return getUri() != null && !getUri().equals("");
    }

    // ---

    /**
     * Checks weather this reference refers to the given topic.
     */
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
