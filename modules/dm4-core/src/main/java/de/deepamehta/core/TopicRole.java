package de.deepamehta.core;



public interface TopicRole extends Role {

    Topic getTopic();

    // ---

    String getTopicUri();

    boolean topicIdentifiedByUri();
}
