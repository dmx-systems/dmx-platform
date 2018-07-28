package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.EventListener;



public interface PostUpdateTopicRequestListener extends EventListener {

    void postUpdateTopicRequest(Topic topic);
}
