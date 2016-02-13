package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.model.topicmaps.ViewProperties;



public interface ViewmodelCustomizer {

    void enrichViewProperties(RelatedTopic topic, ViewProperties viewProps);
}
