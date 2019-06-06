package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckTopicWriteAccess extends EventListener {

    void checkTopicWriteAccess(long topicId);
}
