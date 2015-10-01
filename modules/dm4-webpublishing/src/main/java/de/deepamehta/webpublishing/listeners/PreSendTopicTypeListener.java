package de.deepamehta.webpublishing.listeners;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.EventListener;



public interface PreSendTopicTypeListener extends EventListener {

    void preSendTopicType(TopicType topicType);
}
