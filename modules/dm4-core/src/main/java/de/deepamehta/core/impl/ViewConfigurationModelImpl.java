package de.deepamehta.core.impl;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONArray;

import java.util.Map;
import java.util.logging.Logger;



class ViewConfigurationModelImpl implements ViewConfigurationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Key: config topic type URI
     */
    private Map<String, TopicModel> configTopics;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ViewConfigurationModelImpl(Map<String, TopicModel> configTopics) {
        this.configTopics = configTopics;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterable<TopicModel> getConfigTopics() {
        return configTopics.values();
    }

    @Override
    public void addConfigTopic(TopicModel configTopic) {
        String configTypeUri = configTopic.getTypeUri();
        // error check
        if (getConfigTopic(configTypeUri) != null) {
            throw new RuntimeException("There is already a view configuration topic of type \"" + configTypeUri + "\"");
        }
        //
        configTopics.put(configTypeUri, configTopic);
    }

    @Override
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

    // ---

    @Override
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
    @Override
    public JSONArray toJSONArray() {
        try {
            JSONArray configTopics = new JSONArray();
            for (TopicModel configTopic : getConfigTopics()) {
                configTopics.put(configTopic.toJSON());
            }
            return configTopics;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "view configuration " + configTopics;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicModel getConfigTopic(String configTypeUri) {
        return configTopics.get(configTypeUri);
    }
}
