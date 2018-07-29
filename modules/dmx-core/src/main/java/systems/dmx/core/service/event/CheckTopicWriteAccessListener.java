package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckTopicWriteAccessListener extends EventListener {

    void checkTopicWriteAccess(long topicId);
}
