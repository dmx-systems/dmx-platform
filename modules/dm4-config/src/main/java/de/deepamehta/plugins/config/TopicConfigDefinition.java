package de.deepamehta.plugins.config;

import de.deepamehta.core.model.TopicModel;



public class TopicConfigDefinition extends ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicConfigDefinition(String topicUri, TopicModel defaultConfigTopic, ModificationRole role) {
        super(defaultConfigTopic, role);
        this.topicUri = topicUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    String getConfigurableUri() {
        return topicUri;
    }
}
