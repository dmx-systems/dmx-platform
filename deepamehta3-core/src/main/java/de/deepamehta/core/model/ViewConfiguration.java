package de.deepamehta.core.model;

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
public class ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TopicModel> viewConfig = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ViewConfiguration() {
    }

    /* ### public ViewConfiguration(Set<Topic> topics) {
        for (Topic topic : topics) {
            put(new TopicModel(topic));
        }
    } */

    public ViewConfiguration(Set<RelatedTopic> topics) {
        for (Topic topic : topics) {
            put(new TopicModel(topic));
        }
    }

    public ViewConfiguration(JSONObject configurable) {
        try {
            JSONArray topics = configurable.optJSONArray("view_config_topics");
            if (topics != null) {
                for (int i = 0; i < topics.length(); i++) {
                    put(new TopicModel(topics.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing ViewConfiguration failed (JSONObject=" + configurable + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Iterable<TopicModel> getConfigTopics() {
        return viewConfig.values();
    }

    /**
     * Read out a view configuration setting.
     * <p>
     * Compare to client-side counterpart: function get_view_config() in webclient.js
     *
     * @param   typeUri     The type URI of the configuration topic, e.g. "dm3.webclient.view_config"
     * @param   settingUri  The setting URI, e.g. "dm3.webclient.icon_src"
     *
     * @return  The setting value, or <code>null</code> if there is no such setting
     */
    public Object getSetting(String typeUri, String settingUri) {
        TopicModel configTopic = get(typeUri);
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

    private TopicModel get(String typeUri) {
        return viewConfig.get(typeUri);
    }

    private void put(TopicModel configTopic) {
        viewConfig.put(configTopic.getTypeUri(), configTopic);
    }
}
