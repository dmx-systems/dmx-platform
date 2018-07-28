package de.deepamehta.config;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;



public interface ConfigCustomizer {

    TopicModel getConfigValue(Topic topic);
}
