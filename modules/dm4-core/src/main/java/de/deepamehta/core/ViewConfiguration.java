package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface ViewConfiguration {

    // ### to be dropped from public interface
    Iterable<TopicModel> getConfigTopics();

    // ### to be dropped from public interface
    TopicModel getConfigTopic(String configTypeUri);

    // ### TODO: add a getSetting() method

    void addSetting(String configTypeUri, String settingUri, Object value);

    // ---

    ViewConfigurationModel getModel();
}
