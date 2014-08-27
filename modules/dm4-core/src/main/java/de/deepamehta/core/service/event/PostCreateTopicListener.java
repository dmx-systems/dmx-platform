package de.deepamehta.core.service.event;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.EventListener;



public interface PostCreateTopicListener extends EventListener {

    void postCreateTopic(Topic topic, Directives directives);
}
