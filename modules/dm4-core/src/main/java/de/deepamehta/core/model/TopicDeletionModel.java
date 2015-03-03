package de.deepamehta.core.model;



public class TopicDeletionModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicDeletionModel(long topicId) {
        super(topicId);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "delete " + super.toString();
    }
}
