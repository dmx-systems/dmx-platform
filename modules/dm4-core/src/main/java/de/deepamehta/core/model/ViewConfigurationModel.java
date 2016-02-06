package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface ViewConfigurationModel {

    Iterable<TopicModel> getConfigTopics();

    void addConfigTopic(TopicModel configTopic);

    void updateConfigTopic(TopicModel configTopic);

    // ---

    /**
     * FIXME: to be dropped.
     * <p>
     * Read out a view configuration setting.
     * <p>
     * Compare to client-side counterpart: function get_view_config() in webclient.js
     *
     * @param   configTypeUri   The type URI of the configuration topic, e.g. "dm4.webclient.view_config"
     * @param   settingUri      The setting URI, e.g. "dm4.webclient.icon"
     *
     * @return  The setting value, or <code>null</code> if there is no such setting
     */
    Object getSetting(String configTypeUri, String settingUri);

    // ---

    JSONArray toJSONArray();
}
