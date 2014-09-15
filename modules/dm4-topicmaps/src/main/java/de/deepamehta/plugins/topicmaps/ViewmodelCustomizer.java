package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;



public interface ViewmodelCustomizer {

    void enrichViewProperties(Topic topic, ChildTopicsModel viewProps);

    void storeViewProperties(Topic topic, ChildTopicsModel viewProps);
}
