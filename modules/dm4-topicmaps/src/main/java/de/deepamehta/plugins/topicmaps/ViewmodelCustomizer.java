package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;



public interface ViewmodelCustomizer {

    void modifyViewProperties(Topic topic, CompositeValueModel viewProps);
}
