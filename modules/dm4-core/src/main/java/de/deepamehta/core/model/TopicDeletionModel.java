package de.deepamehta.core.model;



public class TopicDeletionModel extends RelatedTopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicDeletionModel(long topicId) {
        super(topicId);
    }

    public TopicDeletionModel(String topicUri) {
        super(topicUri);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "delete " + super.toString();
    }
}
