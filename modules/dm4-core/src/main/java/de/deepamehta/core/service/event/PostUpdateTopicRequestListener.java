package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Listener;



public interface PostUpdateTopicRequestListener extends Listener {

    void postUpdateTopicRequest(Topic topic);
}
