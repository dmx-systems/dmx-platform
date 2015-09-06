package de.deepamehta.plugins.config;



public abstract class ConfigDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String configTypeUri;
    private ModificationRole role;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ConfigDefinition(String configTypeUri, ModificationRole role) {
        this.configTypeUri = configTypeUri;
        this.role = role;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    abstract String getConfigurableUri();

    String getConfigTypeUri() {
        return configTypeUri;
    }
}
