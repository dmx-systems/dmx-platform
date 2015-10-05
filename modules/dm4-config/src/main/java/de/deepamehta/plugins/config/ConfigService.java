package de.deepamehta.plugins.config;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.plugins.config.ConfigPlugin.ConfigDefinitions;



public interface ConfigService {

    ConfigDefinitions getConfigDefinitions();

    RelatedTopic getConfigTopic(long topicId, String configTypeUri);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);
}
