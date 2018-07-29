package systems.dmx.core.service.event;

import systems.dmx.core.Topic;
import systems.dmx.core.service.EventListener;



public interface PreDeleteTopicListener extends EventListener {

    void preDeleteTopic(Topic topic);
}
