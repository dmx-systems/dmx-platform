package systems.dmx.topicmaps;

import systems.dmx.core.Topic;
import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.core.service.CoreService;



public interface TopicmapType {

    String getUri();

    void initTopicmapState(Topic topicmapTopic, ViewProps viewProps, CoreService dmx);
}
