package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;



/**
 * A container for config topics.
 * <p>
 * Config topics can be accessed by their type URI.
 * A view config can contain only one config topic with a certain type URI.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface ViewConfiguration {

    Iterable<Topic> getConfigTopics();

    /**
     * @return  the config topic for the given type URI, or <code>null</code> if there is none.
     */
    Topic getConfigTopic(String configTypeUri);

    /**
     * Adds a config topic to this view config.
     *
     * @return  the (instantiated) config topic.
     *
     * @throws  RuntimeException    if this view config already contains a config topic with the same type URI.
     */
    Topic addConfigTopic(TopicModel configTopic);

    void addSetting(String configTypeUri, String settingUri, Object value);

    void updateConfigTopic(TopicModel configTopic);

    // ---

    ViewConfigurationModel getModel();
}
