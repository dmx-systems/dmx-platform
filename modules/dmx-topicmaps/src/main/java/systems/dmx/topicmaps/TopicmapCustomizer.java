package systems.dmx.topicmaps;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.topicmaps.ViewProps;



public interface TopicmapCustomizer {

    void customizeTopic(RelatedTopic topic, ViewProps viewProps);
}
