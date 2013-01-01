package de.deepamehta.core;



public interface TopicRole extends Role {

    String getTopicUri();

    boolean topicIdentifiedByUri();

    // ---

    Topic getTopic();
}
