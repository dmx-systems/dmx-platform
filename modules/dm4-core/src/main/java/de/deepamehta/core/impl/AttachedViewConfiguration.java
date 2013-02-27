package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ClientState;



/**
 * A view configuration that is attached to the {@link DeepaMehtaService}.
 */
class AttachedViewConfiguration implements ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final RoleModel configurable;
    private final ViewConfigurationModel model;
    private final EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedViewConfiguration(RoleModel configurable, ViewConfigurationModel model, EmbeddedService dms) {
        this.configurable = configurable;
        this.model = model;
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === ViewConfiguration Implementation ===

    @Override
    public Iterable<TopicModel> getConfigTopics() {
        return model.getConfigTopics();
    }

    @Override
    public TopicModel getConfigTopic(String configTypeUri) {
        return model.getConfigTopic(configTypeUri);
    }

    @Override
    public void addSetting(String configTypeUri, String settingUri, Object value) {
        // update memory
        TopicModel createdTopicModel = model.addSetting(configTypeUri, settingUri, value);
        // update DB
        if (createdTopicModel != null) {
            // Note: a new created view config topic model needs an ID (required for setting up access control).
            // So, the storage layer must operate on that very topic model (instead of creating another one).
            dms.typeStorage.storeViewConfigTopic(configurable, createdTopicModel);
        } else {
            dms.typeStorage.storeViewConfigSetting(configurable, configTypeUri, settingUri, value);
        }
    }

    // ---

    @Override
    public ViewConfigurationModel getModel() {
        return model;
    }
}
