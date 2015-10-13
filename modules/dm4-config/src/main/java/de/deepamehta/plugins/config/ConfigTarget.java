package de.deepamehta.plugins.config;

import de.deepamehta.core.Topic;



public enum ConfigTarget {

    SINGLETON("topic_uri") {
        @Override
        String hashKey(Topic topic) {
            return hashKey(topic.getUri());
        }
    },

    TYPE_INSTANCES("type_uri") {
        @Override
        String hashKey(Topic topic) {
            return hashKey(topic.getTypeUri());
        }
    };

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String prefix;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private ConfigTarget(String prefix) {
        this.prefix = prefix;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    String hashKey(String configurableUri) {
        return prefix + ":" + configurableUri;
    }

    abstract String hashKey(Topic topic);
}
