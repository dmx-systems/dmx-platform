package de.deepamehta.plugins.config;



public class TopicConfigDefinition extends ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicConfigDefinition(String topicUri, String configTypeUri, ModificationRole role) {
        super(configTypeUri, role);
        this.topicUri = topicUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    String getConfigurableUri() {
        return topicUri;
    }
}
