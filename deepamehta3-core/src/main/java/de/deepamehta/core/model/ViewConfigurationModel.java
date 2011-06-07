package de.deepamehta.core.model;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public ViewConfigurationModel(Set<RelatedTopic> configTopics) {
        for (Topic topic : configTopics) {
            addConfigTopic(new TopicModel(topic));
        }
    }

    /**
     * @param   configurable    A topic type or an association definition.
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

    public TopicModel getConfigTopic(String configTypeUri) {
        return viewConfig.get(configTypeUri);
    }

    public void addConfigTopic(TopicModel configTopic) {
        String configTypeUri = configTopic.getTypeUri();
        // error check
        TopicModel existing = getConfigTopic(configTypeUri);
        if (existing != null) {
            throw new RuntimeException("There is already a configuration topic of type \"" + configTypeUri + "\"");
        }
        //
        viewConfig.put(configTypeUri, configTopic);
    }

    public boolean addSetting(String configTypeUri, String settingUri, Object value) {
        boolean configTopicCreated = false;
        // create config topic if not exists
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            configTopic = new TopicModel(configTypeUri);
            addConfigTopic(configTopic);
            configTopicCreated = true;
        }
        // make setting
        configTopic.getComposite().put(settingUri, value);
        //
        return configTopicCreated;
    }

    // ---

    /**
     * FIXME: to be dropped.
     * <p>
     * Read out a view configuration setting.
     * <p>
     * Compare to client-side counterpart: function get_view_config() in webclient.js
     *
     * @param   configTypeUri   The type URI of the configuration topic, e.g. "dm3.webclient.view_config"
     * @param   settingUri      The setting URI, e.g. "dm3.webclient.icon_src"
     *
     * @return  The setting value, or <code>null</code> if there is no such setting
     */
    public Object getSetting(String configTypeUri, String settingUri) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            return null;
        }
        Composite comp = configTopic.getComposite();
        return comp.has(settingUri) ? comp.get(settingUri) : null;
    }

    // ---

    public void toJSON(JSONObject configurable) {
        try {
            Map viewConfigTopics = new HashMap();
            for (TopicModel configTopic : getConfigTopics()) {
                viewConfigTopics.put(configTopic.getTypeUri(), configTopic.toJSON());
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
}
