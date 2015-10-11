package de.deepamehta.plugins.config;

import de.deepamehta.core.model.TopicModel;



public abstract class ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModel defaultConfigTopic;
    private ConfigModificationRole role;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ConfigDefinition(TopicModel defaultConfigTopic, ConfigModificationRole role) {
        this.defaultConfigTopic = defaultConfigTopic;
        this.role = role;
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

    abstract String getConfigurableUri();

    String getConfigTypeUri() {
        return defaultConfigTopic.getTypeUri();
    }

    TopicModel getDefaultConfigTopic() {
        return defaultConfigTopic;
    }

    ConfigModificationRole getConfigModificationRole() {
        return role;
    }
}
