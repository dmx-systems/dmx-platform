package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.EventListener;



public interface PreSendTopicListener extends EventListener {

    void preSendTopic(Topic topic);
}
