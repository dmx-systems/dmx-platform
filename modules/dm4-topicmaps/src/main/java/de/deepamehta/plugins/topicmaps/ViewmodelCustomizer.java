package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;



public interface ViewmodelCustomizer {

    void enrichViewProperties(Topic topic, CompositeValueModel viewProps);

    void storeViewProperties(Topic topic, CompositeValueModel viewProps);
}
