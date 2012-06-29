package de.deepamehta.core.service.listeners;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PostCreateTopicListener extends Listener {

    void postCreateTopic(Topic topic, ClientState clientState, Directives directives);
}
