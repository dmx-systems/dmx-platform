package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.EventListener;



public interface PreUpdateTopicListener extends EventListener {

    void preUpdateTopic(Topic topic, TopicModel newModel, Directives directives);
}
