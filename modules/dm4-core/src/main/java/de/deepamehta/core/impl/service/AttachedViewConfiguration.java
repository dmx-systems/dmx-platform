package de.deepamehta.core.impl.service;

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
    public void addConfigTopic(TopicModel configTopic) {
        // update memory
        model.addConfigTopic(configTopic);
        // update DB
        // ### storeConfigTopic(..., configTopic);          // ### FIXME: a view config doesn't know its parent
        throw new RuntimeException("not yet implemented");  // addConfigTopic() is currently not used
    }

    @Override
    public void addSetting(String configTypeUri, String settingUri, Object value) {
        // update memory
        model.addSetting(configTypeUri, settingUri, value);
        // update DB
        dms.objectFactory.storeViewConfigSetting(configurable, configTypeUri, settingUri, value);
    }

    // ---

    @Override
    public ViewConfigurationModel getModel() {
        return model;
    }
}
