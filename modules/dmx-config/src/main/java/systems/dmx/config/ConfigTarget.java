package systems.dmx.config;

import systems.dmx.core.Topic;



public enum ConfigTarget {

    SINGLETON("topicUri") {
        @Override
        String hashKey(Topic topic) {
            return hashKey(topic.getUri());
        }
    },

    TYPE_INSTANCES("typeUri") {
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
