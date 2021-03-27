package systems.dmx.config;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;



public interface ConfigService {

    RelatedTopic getConfigTopic(String configTypeUri, long topicId);

    void updateConfigTopic(long topicId, TopicModel updateModel);

    void createConfigTopic(String configTypeUri, Topic topic);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);
}
