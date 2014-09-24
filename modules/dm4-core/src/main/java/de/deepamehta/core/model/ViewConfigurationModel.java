package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class ViewConfigurationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Key: config topic type URI
     */
    private Map<String, TopicModel> viewConfig = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ViewConfigurationModel() {
    }

    public ViewConfigurationModel(List<TopicModel> configTopics) {
        for (TopicModel topic : configTopics) {
            addConfigTopic(topic);
        }
    }

    /**
     * @param   configurable    A topic type, an association type, or an association definition.
     *                          ### FIXME: the sole JSONArray should be passed
     */
    public ViewConfigurationModel(JSONObject configurable) {
        try {
            JSONArray topics = configurable.optJSONArray("view_config_topics");
            if (topics != null) {
                for (int i = 0; i < topics.length(); i++) {
                    addConfigTopic(new TopicModel(topics.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing ViewConfigurationModel failed (JSONObject=" + configurable + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Iterable<TopicModel> getConfigTopics() {
        return viewConfig.values();
    }

    public void updateConfigTopic(TopicModel configTopic) {
        String configTypeUri = configTopic.getTypeUri();
        TopicModel confTopic = getConfigTopic(configTypeUri);
        // error check
        if (confTopic == null) {
            throw new RuntimeException("There is no view configuration topic of type \"" + configTypeUri + "\"");
        }
        //
        confTopic.set(configTopic);
    }

    public TopicModel addSetting(String configTypeUri, String settingUri, Object value) {
        // create config topic if not exists
        boolean created = false;
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            configTopic = new TopicModel(configTypeUri);
            addConfigTopic(configTopic);
            created = true;
        }
        // make setting
        configTopic.getChildTopicsModel().put(settingUri, value);
        //
        return created ? configTopic : null;
    }

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
    public Object getSetting(String configTypeUri, String settingUri) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            return null;
        }
        ChildTopicsModel childTopics = configTopic.getChildTopicsModel();
        return childTopics.has(settingUri) ? childTopics.getObject(settingUri) : null;
    }

    // ---

    // ### FIXME: drop parameter, implement JSONEnabled
    public void toJSON(JSONObject configurable) {
        try {
            List viewConfigTopics = new ArrayList();
            for (TopicModel configTopic : getConfigTopics()) {
                viewConfigTopics.add(configTopic.toJSON());
            }
            configurable.put("view_config_topics", viewConfigTopics);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "view configuration " + viewConfig;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicModel getConfigTopic(String configTypeUri) {
        return viewConfig.get(configTypeUri);
    }

    private void addConfigTopic(TopicModel configTopic) {
        String configTypeUri = configTopic.getTypeUri();
        // error check
        if (getConfigTopic(configTypeUri) != null) {
            throw new RuntimeException("There is already a view configuration topic of type \"" + configTypeUri + "\"");
        }
        //
        viewConfig.put(configTypeUri, configTopic);
    }
}
