package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface CheckTopicWriteAccessListener extends EventListener {

    void checkTopicWriteAccess(long topicId);
}
