package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.CompositeValueModel;



public interface TopicmapRenderer {

    String getUri();

    CompositeValueModel initialTopicmapState();
}
