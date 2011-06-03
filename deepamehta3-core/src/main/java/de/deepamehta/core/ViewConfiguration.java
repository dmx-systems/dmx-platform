package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface ViewConfiguration {

    Iterable<TopicModel> getConfigTopics();

    void addConfigTopic(TopicModel configTopic);
}
