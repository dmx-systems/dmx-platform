package systems.dmx.core.impl;

import systems.dmx.core.Topic;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.ViewConfigurationModel;



/**
 * A view configuration that is attached to the {@link AccessLayer}.
 */
class ViewConfigurationImpl implements ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The underlying model.
     */
    private ViewConfigurationModelImpl model;

    /**
     * A player that points to the object this view configuration applies to.
     * This is either a type (topic player) or a comp def (association player).
     */
    private PlayerModel configurable;

    private AccessLayer al;
    private ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ViewConfigurationImpl(PlayerModel configurable, ViewConfigurationModelImpl model, AccessLayer al) {
        this.configurable = configurable;
        this.model = model;
        this.al = al;
        this.mf = al.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === ViewConfiguration Implementation ===

    @Override
    public Iterable<Topic> getConfigTopics() {
        return al.instantiate(model.getConfigTopics());
    }

    @Override
    public Topic getConfigTopic(String configTypeUri) {
        TopicModelImpl configTopic = model.getConfigTopic(configTypeUri);
        return configTopic != null ? configTopic.instantiate() : null;
    }

    @Override
    public Topic addConfigTopic(TopicModel configTopic) {
        TopicModelImpl _configTopic = (TopicModelImpl) configTopic;
        _addConfigTopic(_configTopic);                      // update memory + DB
        return _configTopic.instantiate();
    }

    @Override
    public ViewConfiguration setConfigValue(String configTypeUri, String childTypeUri, Object value) {
        _setConfigValue(configTypeUri, mf.newChildTopicsModel()
            .set(childTypeUri, value));
        return this;
    }

    @Override
    public ViewConfiguration setConfigValueRef(String configTypeUri, String childTypeUri, Object topicIdOrUri) {
        _setConfigValue(configTypeUri, mf.newChildTopicsModel()
            .set(childTypeUri, mf.newTopicReferenceModel(topicIdOrUri)));
        return this;
    }

    // ---

    @Override
    public ViewConfigurationModel getModel() {
        return model;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void _setConfigValue(String configTypeUri, ChildTopicsModel children) {
        TopicModelImpl configTopic = model.getConfigTopic(configTypeUri);
        if (configTopic == null) {
            configTopic = mf.newTopicModel(configTypeUri, children);
            _addConfigTopic(configTopic);       // update memory + DB
        } else {
            configTopic.update(children);       // update memory + DB
        }
    }

    private void _addConfigTopic(TopicModelImpl configTopic) {
        model.addConfigTopic(configTopic);                                  // update memory
        al.typeStorage.storeViewConfigTopic(configurable, configTopic);     // update DB
    }
}
