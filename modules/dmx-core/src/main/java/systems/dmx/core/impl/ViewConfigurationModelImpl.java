package systems.dmx.core.impl;

import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONArray;

import java.util.Map;
import java.util.logging.Logger;



class ViewConfigurationModelImpl implements ViewConfigurationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Key: config topic type URI
     * Value: config topic
     */
    private Map<String, TopicModelImpl> configTopics;

    private ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   configTopics    must not be null
     */
    ViewConfigurationModelImpl(Map<String, TopicModelImpl> configTopics, PersistenceLayer pl) {
        this.configTopics = configTopics;
        this.mf = pl.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterable<TopicModelImpl> getConfigTopics() {
        return configTopics.values();
    }

    @Override
    public TopicModelImpl getConfigTopic(String configTypeUri) {
        return configTopics.get(configTypeUri);
    }

    @Override
    public ViewConfigurationModel addConfigTopic(TopicModel configTopic) {
        // error check
        String configTypeUri = configTopic.getTypeUri();
        if (getConfigTopic(configTypeUri) != null) {
            throw new RuntimeException("There is already a view configuration topic of type \"" + configTypeUri + "\"");
        }
        //
        putConfigTopic(configTopic);
        return this;
    }

    @Override
    public void updateConfigTopic(TopicModel configTopic) {
        // error check
        String configTypeUri = configTopic.getTypeUri();
        if (getConfigTopic(configTypeUri) == null) {
            throw new RuntimeException("There is no view configuration topic of type \"" + configTypeUri + "\"");
        }
        //
        putConfigTopic(configTopic);
    }

    // ---

    @Override
    public ViewConfigurationModel setConfigValue(String configTypeUri, String childTypeUri, Object value) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            addConfigTopic(mf.newTopicModel(configTypeUri, mf.newChildTopicsModel().put(childTypeUri, value)));
        } else {
            configTopic.getChildTopicsModel().put(childTypeUri, value);
        }
        return this;
    }

    @Override
    public ViewConfigurationModel setConfigValueRef(String configTypeUri, String childTypeUri, Object topicIdOrUri) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        RelatedTopicModel valueRef = mf.newTopicReferenceModel(topicIdOrUri);
        if (configTopic == null) {
            // Note: valueRef must be known to be of type RelatedTopicModel *at compile time*.
            // Otherwise the wrong put() method would be invoked.
            // In Java method overloading involves NO dynamic dispatch. See JavaAPITest in dmx-test.
            addConfigTopic(mf.newTopicModel(configTypeUri, mf.newChildTopicsModel().put(childTypeUri, valueRef)));
        } else {
            configTopic.getChildTopicsModel().put(childTypeUri, valueRef);
        }
        return this;
    }

    // ---

    @Override
    public Object getConfigValue(String configTypeUri, String childTypeUri) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            return null;
        }
        return configTopic.getChildTopicsModel().getObject(childTypeUri, null);
    }

    // ---

    @Override
    public JSONArray toJSONArray() {
        try {
            JSONArray configTopics = new JSONArray();
            for (TopicModel configTopic : getConfigTopics()) {
                configTopics.put(configTopic.toJSON());
            }
            return configTopics;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public String toString() {
        return "view configuration " + configTopics;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    public void putConfigTopic(TopicModel configTopic) {
        configTopics.put(configTopic.getTypeUri(), (TopicModelImpl) configTopic);
    }
}
