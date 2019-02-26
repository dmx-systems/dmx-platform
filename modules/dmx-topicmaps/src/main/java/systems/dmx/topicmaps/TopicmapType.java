package systems.dmx.topicmaps;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.ModelFactory;



public interface TopicmapType {

    String getUri();

    ChildTopicsModel initialTopicmapState(ModelFactory mf);
}
