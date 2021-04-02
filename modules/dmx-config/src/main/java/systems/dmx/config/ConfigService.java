package systems.dmx.config;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.DirectivesResponse;



public interface ConfigService {

    ConfigDefs getConfigDefs();

    RelatedTopic getConfigTopic(String configTypeUri, long topicId);

    DirectivesResponse updateConfigTopic(long topicId, TopicModel updateModel);

    void createConfigTopic(String configTypeUri, Topic topic);

    // ---

    void registerConfigDef(ConfigDef configDef);

    void unregisterConfigDef(String configTypeUri);
}
