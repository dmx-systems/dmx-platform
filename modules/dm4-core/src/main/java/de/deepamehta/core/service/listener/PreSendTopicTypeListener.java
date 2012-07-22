package de.deepamehta.core.service.listener;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface PreSendTopicTypeListener extends Listener {

    void preSendTopicType(TopicType topicType, ClientState clientState);
}
