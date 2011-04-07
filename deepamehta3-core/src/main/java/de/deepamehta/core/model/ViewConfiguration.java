package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
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

    private Set<TopicData> viewConfig = new HashSet();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ViewConfiguration() {
    }

    public ViewConfiguration(Set<Topic> topics) {
        for (Topic topic : topics) {
            viewConfig.add(new TopicData(topic));
        }
    }

    public ViewConfiguration(JSONObject configurable) {
        try {
            JSONArray topics = configurable.optJSONArray("view_config_topics");
            if (topics != null) {
                for (int i = 0; i < topics.length(); i++) {
                    viewConfig.add(new TopicData(topics.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing ViewConfiguration failed (JSONObject=" + configurable + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Iterable<TopicData> getTopicData() {
        return viewConfig;
    }

    // ---

    public void toJSON(JSONObject configurable) {
        try {
            Map viewConfigTopics = new HashMap();
            for (TopicData topicData : viewConfig) {
                viewConfigTopics.put(topicData.getTypeUri(), topicData.toJSON());
            }
            configurable.put("view_config_topics", viewConfigTopics);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "view configuration " + viewConfig.toString();
    }
}
