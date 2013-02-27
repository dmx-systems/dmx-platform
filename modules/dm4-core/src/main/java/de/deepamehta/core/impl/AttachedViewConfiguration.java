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

    private final RoleModel configurable;
    private final ViewConfigurationModel model;
    private final EmbeddedService dms;

    private Map<String, Topic> configTopics = new HashMap();    // attached object cache

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
        // update memory
        TopicModel createdTopicModel = model.addSetting(configTypeUri, settingUri, value);
        // update DB
        if (createdTopicModel != null) {
            // attached object cache
            addConfigTopic(createdTopicModel);
            // Note: a new created view config topic model needs an ID (required for setting up access control).
            // So, the storage layer must operate on that very topic model (instead of creating another one).
            dms.typeStorage.storeViewConfigTopic(configurable, createdTopicModel);
        } else {
            dms.typeStorage.storeViewConfigSetting(configurable, configTypeUri, settingUri, value);
        }
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
            addConfigTopic(configTopic);
        }
    }

    private void addConfigTopic(TopicModel configTopic) {
            configTopics.put(configTopic.getTypeUri(), new AttachedTopic(configTopic, dms));
    }
}
