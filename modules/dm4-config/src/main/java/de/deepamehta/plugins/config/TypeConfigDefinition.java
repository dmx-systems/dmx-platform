package de.deepamehta.plugins.config;



public class TypeConfigDefinition extends ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String topicTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TypeConfigDefinition(String topicTypeUri, String configTypeUri, ModificationRole role) {
        super(configTypeUri, role);
        this.topicTypeUri = topicTypeUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    String getConfigurableUri() {
        return topicTypeUri;
    }
}
