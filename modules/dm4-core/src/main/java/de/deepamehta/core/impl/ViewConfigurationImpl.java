package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ModelFactory;

import java.util.HashMap;
import java.util.Map;



/**
 * A view configuration that is attached to the {@link DeepaMehtaService}.
 */
class ViewConfigurationImpl implements ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, Topic> configTopics = new HashMap();    // attached object cache

    private ViewConfigurationModel model;                       // underlying model
    private RoleModel configurable;

    private PersistenceLayer pl;
    private ModelFactory mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ViewConfigurationImpl(RoleModel configurable, ViewConfigurationModel model, PersistenceLayer pl) {
        this.configurable = configurable;
        this.model = model;
        this.pl = pl;
        this.mf = pl.mf;
        initConfigTopics();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === ViewConfiguration Implementation ===

    @Override
    public Iterable<Topic> getConfigTopics() {
        return configTopics.values();
    }

    @Override
    public void addSetting(String configTypeUri, String settingUri, Object value) {
        Topic configTopic = getConfigTopic(configTypeUri);
        if (configTopic == null) {
            // update DB
            configTopic = pl.typeStorage.storeViewConfigTopic(configurable, mf.newTopicModel(configTypeUri));
            // update memory
            model.addConfigTopic(configTopic.getModel());
            // update attached object cache
            addConfigTopic(configTopic);
        }
        configTopic.getChildTopics().set(settingUri, value);
    }

    @Override
    public void updateConfigTopic(TopicModel configTopic) {
        model.updateConfigTopic(configTopic);
    }

    // ---

    @Override
    public ViewConfigurationModel getModel() {
        return model;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initConfigTopics() {
        for (TopicModel configTopic : model.getConfigTopics()) {
            addConfigTopic(new TopicImpl((TopicModelImpl) configTopic, pl));
        }
    }

    // ---

    private Topic getConfigTopic(String configTypeUri) {
        return configTopics.get(configTypeUri);
    }

    private void addConfigTopic(Topic configTopic) {
        configTopics.put(configTopic.getTypeUri(), configTopic);
    }
}
