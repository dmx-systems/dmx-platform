package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.EventListener;



public interface PreDeleteTopicListener extends EventListener {

    void preDeleteTopic(Topic topic, Directives directives);
}
