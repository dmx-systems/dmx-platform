package de.deepamehta.config;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;



public interface ConfigService {

    RelatedTopic getConfigTopic(String configTypeUri, long topicId);

    void createConfigTopic(String configTypeUri, Topic topic);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);
}
