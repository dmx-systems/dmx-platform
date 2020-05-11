package systems.dmx.core.impl;

import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.ViewConfigModel;

import org.codehaus.jettison.json.JSONArray;

import java.util.Map;
import java.util.logging.Logger;



class ViewConfigModelImpl implements ViewConfigModel {

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
    ViewConfigModelImpl(Map<String, TopicModelImpl> configTopics, AccessLayer al) {
        this.configTopics = configTopics;
        this.mf = al.mf;
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
    public ViewConfigModel addConfigTopic(TopicModel configTopic) {
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
    public ViewConfigModel setConfigValue(String configTypeUri, String childTypeUri, Object value) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            addConfigTopic(mf.newTopicModel(configTypeUri, mf.newChildTopicsModel().set(childTypeUri, value)));
        } else {
            configTopic.getChildTopics().set(childTypeUri, value);
        }
        return this;
    }

    @Override
    public ViewConfigModel setConfigValueRef(String configTypeUri, String childTypeUri, Object topicIdOrUri) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        RelatedTopicModel valueRef = mf.newTopicReferenceModel(topicIdOrUri);
        if (configTopic == null) {
            // Note: valueRef must be known to be of type RelatedTopicModel *at compile time*.
            // Otherwise the wrong set() method would be invoked.
            // In Java method overloading involves NO dynamic dispatch. See JavaAPITest in dmx-test.
            addConfigTopic(mf.newTopicModel(configTypeUri, mf.newChildTopicsModel().set(childTypeUri, valueRef)));
        } else {
            configTopic.getChildTopics().set(childTypeUri, valueRef);
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
        return configTopic.getChildTopics().getValue(childTypeUri, null);
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
