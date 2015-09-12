package de.deepamehta.plugins.config;

import de.deepamehta.core.model.TopicModel;



public abstract class ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModel defaultConfigTopic;
    private ModificationRole role;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ConfigDefinition(TopicModel defaultConfigTopic, ModificationRole role) {
        this.defaultConfigTopic = defaultConfigTopic;
        this.role = role;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract String getConfigurableUri();

    String getConfigTypeUri() {
        return defaultConfigTopic.getTypeUri();
    }

    TopicModel getDefaultConfigTopic() {
        return defaultConfigTopic;
    }
}
