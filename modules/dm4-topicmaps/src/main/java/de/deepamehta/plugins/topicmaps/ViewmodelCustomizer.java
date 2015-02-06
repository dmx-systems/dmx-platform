package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.ViewProperties;
import de.deepamehta.core.Topic;



public interface ViewmodelCustomizer {

    void enrichViewProperties(Topic topic, ViewProperties viewProps);
}
