package systems.dmx.topicmaps;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.topicmaps.ViewProps;



public interface ViewmodelCustomizer {

    void enrichViewProps(RelatedTopic topic, ViewProps viewProps);
}
