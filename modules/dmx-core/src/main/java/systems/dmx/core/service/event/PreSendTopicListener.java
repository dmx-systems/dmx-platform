package systems.dmx.core.service.event;

import systems.dmx.core.Topic;
import systems.dmx.core.service.EventListener;



public interface PreSendTopicListener extends EventListener {

    void preSendTopic(Topic topic);
}
