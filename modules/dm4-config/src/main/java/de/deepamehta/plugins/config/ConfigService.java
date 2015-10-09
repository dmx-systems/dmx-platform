package de.deepamehta.plugins.config;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.plugins.config.ConfigPlugin.ConfigDefinitions;



public interface ConfigService {

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);

    // ---

    RelatedTopic getConfigTopic(long topicId, String configTypeUri);

    RelatedTopic getConfigTopic(String topicUri, String configTypeUri);

    // ---

    ConfigDefinitions getConfigDefinitions();
}
