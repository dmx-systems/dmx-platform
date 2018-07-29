package systems.dmx.core.service.event;

import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.EventListener;



public interface PreUpdateTopicListener extends EventListener {

    void preUpdateTopic(Topic topic, TopicModel updateModel);
}
