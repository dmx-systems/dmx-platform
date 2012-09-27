package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PreUpdateTopicListener extends Listener {

    void preUpdateTopic(Topic topic, TopicModel newModel, Directives directives);
}
