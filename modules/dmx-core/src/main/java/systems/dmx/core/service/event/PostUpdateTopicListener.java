package systems.dmx.core.service.event;

import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.EventListener;



public interface PostUpdateTopicListener extends EventListener {

    void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic);
}
