package de.deepamehta.plugins.config;

import de.deepamehta.core.model.TopicModel;



public class TypeConfigDefinition extends ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TypeConfigDefinition(String topicTypeUri, TopicModel defaultConfigTopic, ConfigModificationRole role) {
        super(defaultConfigTopic, role);
        this.topicTypeUri = topicTypeUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    String getConfigurableUri() {
        return topicTypeUri;
    }
}
