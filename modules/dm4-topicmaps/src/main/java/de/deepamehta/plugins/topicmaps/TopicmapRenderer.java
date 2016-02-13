package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.service.ModelFactory;



public interface TopicmapRenderer {

    String getUri();

    ChildTopicsModel initialTopicmapState(ModelFactory mf);
}
