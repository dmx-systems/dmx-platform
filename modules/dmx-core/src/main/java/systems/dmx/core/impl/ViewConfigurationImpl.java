package systems.dmx.core.impl;

import systems.dmx.core.Topic;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.ViewConfigurationModel;



/**
 * A view configuration that is attached to the {@link PersistenceLayer}.
 */
class ViewConfigurationImpl implements ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The underlying model.
     */
    private ViewConfigurationModelImpl model;

    /**
     * A role that points to the object this view configuration applies to.
     * This is either a type (topic role) or a comp def (association role).
     */
    private PlayerModel configurable;

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ViewConfigurationImpl(PlayerModel configurable, ViewConfigurationModelImpl model, PersistenceLayer pl) {
        this.configurable = configurable;
        this.model = model;
        this.pl = pl;
        this.mf = pl.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === ViewConfiguration Implementation ===

    @Override
    public Iterable<Topic> getConfigTopics() {
        return pl.instantiate(model.getConfigTopics());
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
            .put(childTypeUri, value));
        return this;
    }

    @Override
    public ViewConfiguration setConfigValueRef(String configTypeUri, String childTypeUri, Object topicIdOrUri) {
        _setConfigValue(configTypeUri, mf.newChildTopicsModel()
            .put(childTypeUri, mf.newTopicReferenceModel(topicIdOrUri)));
        return this;
    }

    // ---

    @Override
    public ViewConfigurationModel getModel() {
        return model;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void _setConfigValue(String configTypeUri, ChildTopicsModel childs) {
        TopicModelImpl configTopic = model.getConfigTopic(configTypeUri);
        if (configTopic == null) {
            configTopic = mf.newTopicModel(configTypeUri, childs);
            _addConfigTopic(configTopic);               // update memory + DB
        } else {
            configTopic.updateChildTopics(childs);      // update memory + DB
        }
    }

    private void _addConfigTopic(TopicModelImpl configTopic) {
        model.addConfigTopic(configTopic);                                  // update memory
        pl.typeStorage.storeViewConfigTopic(configurable, configTopic);     // update DB
    }
}
