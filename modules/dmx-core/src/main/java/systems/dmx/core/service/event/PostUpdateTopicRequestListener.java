package systems.dmx.core.service.event;

import systems.dmx.core.Topic;
import systems.dmx.core.service.EventListener;



public interface PostUpdateTopicRequestListener extends EventListener {

    void postUpdateTopicRequest(Topic topic);
}
