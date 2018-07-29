package systems.dmx.core.service.event;

import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.EventListener;



public interface PostDeleteTopicListener extends EventListener {

    void postDeleteTopic(TopicModel topic);
}
