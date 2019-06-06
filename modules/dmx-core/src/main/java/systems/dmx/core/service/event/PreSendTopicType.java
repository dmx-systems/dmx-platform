package systems.dmx.core.service.event;

import systems.dmx.core.TopicType;
import systems.dmx.core.service.EventListener;



public interface PreSendTopicType extends EventListener {

    void preSendTopicType(TopicType topicType);
}
