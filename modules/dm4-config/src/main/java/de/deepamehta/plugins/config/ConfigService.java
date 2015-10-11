package de.deepamehta.plugins.config;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;



public interface ConfigService {

    RelatedTopic getConfigTopic(String configTypeUri, long topicId);

    RelatedTopic getConfigTopic(String configTypeUri, String topicUri);

    RelatedTopic getConfigTopic(String configTypeUri, Topic topic);

    // ---

    void createConfigTopic(String configTypeUri, Topic topic);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);

    // ---

    ConfigDefinitions getConfigDefinitions();
}
