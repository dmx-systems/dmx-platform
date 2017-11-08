package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;



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
     * This is either a type (topic role) or an association definition (association role).
     */
    private RoleModel configurable;

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ViewConfigurationImpl(RoleModel configurable, ViewConfigurationModelImpl model, PersistenceLayer pl) {
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
    public void setConfigValue(String configTypeUri, String childTypeUri, Object value) {
        _setConfigValue(configTypeUri, mf.newChildTopicsModel()
            .put(childTypeUri, value));
    }

    @Override
    public void setConfigValueRef(String configTypeUri, String childTypeUri, Object topicIdOrUri) {
        _setConfigValue(configTypeUri, mf.newChildTopicsModel()
            .put(childTypeUri, mf.newTopicReferenceModel(topicIdOrUri)));
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
            _addConfigTopic(configTopic);                   // update memory + DB
        } else {
            configTopic.updateWithChildTopics(childs);      // update memory + DB
        }
    }

    private void _addConfigTopic(TopicModelImpl configTopic) {
        model.addConfigTopic(configTopic);                                  // update memory
        pl.typeStorage.storeViewConfigTopic(configurable, configTopic);     // update DB
    }
}
