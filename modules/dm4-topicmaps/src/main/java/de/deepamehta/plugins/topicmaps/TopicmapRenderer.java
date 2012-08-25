package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.CompositeValue;



public interface TopicmapRenderer {

    String getUri();

    CompositeValue initialTopicmapState();
}
