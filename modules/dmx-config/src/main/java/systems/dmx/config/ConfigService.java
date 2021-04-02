package systems.dmx.config;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.DirectivesResponse;



public interface ConfigService {

    ConfigDefinitions getConfigDefs();

    RelatedTopic getConfigTopic(String configTypeUri, long topicId);

    DirectivesResponse updateConfigTopic(long topicId, TopicModel updateModel);

    void createConfigTopic(String configTypeUri, Topic topic);

    // ---

    void registerConfigDefinition(ConfigDefinition configDef);

    void unregisterConfigDefinition(String configTypeUri);
}
