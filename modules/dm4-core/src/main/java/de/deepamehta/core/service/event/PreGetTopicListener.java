package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface PreGetTopicListener extends EventListener {

    void preGetTopic(long topicId);
}
