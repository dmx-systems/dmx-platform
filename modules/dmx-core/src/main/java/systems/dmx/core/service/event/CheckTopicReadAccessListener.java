package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckTopicReadAccessListener extends EventListener {

    void checkTopicReadAccess(long topicId);
}
