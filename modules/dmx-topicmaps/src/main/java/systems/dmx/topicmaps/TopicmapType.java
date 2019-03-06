package systems.dmx.topicmaps;

import systems.dmx.core.model.topicmaps.ViewProps;



public interface TopicmapType {

    String getUri();

    void initTopicmapState(ViewProps viewProps);
}
