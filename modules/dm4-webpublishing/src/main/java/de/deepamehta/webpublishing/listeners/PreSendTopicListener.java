package de.deepamehta.webpublishing.listeners;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.EventListener;



public interface PreSendTopicListener extends EventListener {

    void preSendTopic(Topic topic);
}
