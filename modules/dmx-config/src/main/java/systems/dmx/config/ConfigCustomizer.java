package systems.dmx.config;

import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;



public interface ConfigCustomizer {

    TopicModel getConfigValue(Topic topic);
}
