package de.deepamehta.core.service.event;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.EventListener;



public interface PreSendTopicTypeListener extends EventListener {

    void preSendTopicType(TopicType topicType, ClientState clientState);
}
