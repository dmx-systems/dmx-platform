package systems.dmx.core.service.event;

import systems.dmx.core.TopicType;
import systems.dmx.core.service.EventListener;



public interface PreSendTopicTypeListener extends EventListener {

    void preSendTopicType(TopicType topicType);
}
