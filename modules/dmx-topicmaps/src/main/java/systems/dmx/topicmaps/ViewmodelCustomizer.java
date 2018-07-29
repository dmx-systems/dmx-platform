package systems.dmx.topicmaps;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.topicmaps.ViewProperties;



public interface ViewmodelCustomizer {

    void enrichViewProperties(RelatedTopic topic, ViewProperties viewProps);
}
