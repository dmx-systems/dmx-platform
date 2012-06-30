package de.deepamehta.core.service.listener;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface PreSendTopicListener extends Listener {

    void preSendTopic(Topic topic, ClientState clientState);
}
