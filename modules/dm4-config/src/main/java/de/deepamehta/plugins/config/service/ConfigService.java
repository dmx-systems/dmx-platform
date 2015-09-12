package de.deepamehta.plugins.config.service;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.plugins.config.ConfigDefinition;
import de.deepamehta.plugins.config.ConfigPlugin.ConfigDefinitions;



public interface ConfigService extends PluginService {

    ConfigDefinitions getConfigDefinitions();

    RelatedTopic getConfigTopic(long topicId, String configTypeUri);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);
}
