package de.deepamehta.core.service.event;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.EventListener;



public interface PreCreateTopicListener extends EventListener {

    void preCreateTopic(TopicModel model, ClientState clientState);
}
