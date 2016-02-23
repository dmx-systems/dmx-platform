package de.deepamehta.core.service.event;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.EventListener;



public interface PostDeleteTopicListener extends EventListener {

    void postDeleteTopic(TopicModel topic);
}
