package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import java.util.HashMap;
import java.util.Map;



/**
 * A view configuration that is attached to the {@link DeepaMehtaService}.
 */
class AttachedViewConfiguration implements ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, Topic> configTopics = new HashMap();    // attached object cache

    private final ViewConfigurationModel model;                 // underlying model
    private final RoleModel configurable;

    private final EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedViewConfiguration(RoleModel configurable, ViewConfigurationModel model, EmbeddedService dms) {
        this.configurable = configurable;
        this.model = model;
        this.dms = dms;
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
            configTopic = dms.typeStorage.storeViewConfigTopic(configurable, new TopicModel(configTypeUri));
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
            addConfigTopic(new AttachedTopic(configTopic, dms));
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
