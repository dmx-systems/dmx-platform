package de.deepamehta.core.model;



public class TopicDeletionModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicDeletionModel(long topicId) {
        super(topicId);
    }

    public TopicDeletionModel(String topicUri) {
        super(topicUri, (String) null);     // typeUri=null
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "delete " + super.toString();
    }
}
