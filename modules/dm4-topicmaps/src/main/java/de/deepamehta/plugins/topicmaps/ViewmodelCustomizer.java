package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.ViewProperties;
import de.deepamehta.core.RelatedTopic;



public interface ViewmodelCustomizer {

    void enrichViewProperties(RelatedTopic topic, ViewProperties viewProps);
}
