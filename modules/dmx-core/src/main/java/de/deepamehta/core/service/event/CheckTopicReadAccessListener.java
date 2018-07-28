package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface CheckTopicReadAccessListener extends EventListener {

    void checkTopicReadAccess(long topicId);
}
