package systems.dmx.config;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;



public interface ConfigService {

    RelatedTopic getConfigTopic(String configTypeUri, long topicId);

    void createConfigTopic(String configTypeUri, Topic topic);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);
}
