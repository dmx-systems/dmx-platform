package de.deepamehta.topicmaps;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.service.ModelFactory;



// ### TODO: rename to "TopicmapType"
public interface TopicmapRenderer {

    String getUri();

    ChildTopicsModel initialTopicmapState(ModelFactory mf);
}
