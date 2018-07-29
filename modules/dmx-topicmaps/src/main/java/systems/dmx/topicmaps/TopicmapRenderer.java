package systems.dmx.topicmaps;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.ModelFactory;



// ### TODO: rename to "TopicmapType"
public interface TopicmapRenderer {

    String getUri();

    ChildTopicsModel initialTopicmapState(ModelFactory mf);
}
