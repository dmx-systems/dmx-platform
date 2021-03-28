package systems.dmx.config;

import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;



public class ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ConfigTarget target;
    private String configurableUri;
    private TopicModel defaultConfigTopic;
    private ConfigModificationRole role;
    private ConfigCustomizer customizer;

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   configurableUri     either a topic URI or a topic type URI, depending on "target".
     */
    public ConfigDefinition(ConfigTarget target, String configurableUri, TopicModel defaultConfigTopic,
                                                                         ConfigModificationRole role) {
        this(target, configurableUri, defaultConfigTopic, role, null);
    }

    /**
     * @param   configurableUri     either a topic URI or a topic type URI, depending on "target".
     */
    public ConfigDefinition(ConfigTarget target, String configurableUri, TopicModel defaultConfigTopic,
                                                 ConfigModificationRole role, ConfigCustomizer customizer) {
        this.target = target;
        this.configurableUri = configurableUri;
        this.defaultConfigTopic = defaultConfigTopic;
        this.role = role;
        this.customizer = customizer;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public boolean equals(Object o) {
        return getConfigTypeUri().equals(((ConfigDefinition) o).getConfigTypeUri());
    }

    @Override
    public int hashCode() {
        return getConfigTypeUri().hashCode();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    String getHashKey() {
        return target.hashKey(configurableUri);
    }

    String getConfigTypeUri() {
        return defaultConfigTopic.getTypeUri();
    }

    TopicModel getConfigValue(Topic topic) {
        if (customizer != null) {
            TopicModel configValue = customizer.getConfigValue(topic);
            if (configValue != null) {
                return configValue;
            }
        }
        return defaultConfigTopic;
    }

    ConfigModificationRole getConfigModificationRole() {
        return role;
    }
}
