package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface ViewConfiguration {

    Iterable<TopicModel> getConfigTopics();

    TopicModel getConfigTopic(String configTypeUri);

    void addConfigTopic(TopicModel configTopic);

    void addSetting(String configTypeUri, String settingUri, Object value);
}
