package de.deepamehta.core;



public interface TopicRole extends Role {

    long getTopicId();

    String getTopicUri();

    boolean topicIdentifiedByUri();
}
