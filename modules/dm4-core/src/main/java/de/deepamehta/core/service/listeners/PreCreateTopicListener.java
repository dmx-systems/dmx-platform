package de.deepamehta.core.service.listeners;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface PreCreateTopicListener extends Listener {

    void preCreateTopic(TopicModel model, ClientState clientState);
}
