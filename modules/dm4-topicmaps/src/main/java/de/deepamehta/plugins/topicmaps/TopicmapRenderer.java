package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.ChildTopicsModel;



public interface TopicmapRenderer {

    String getUri();

    ChildTopicsModel initialTopicmapState();
}
