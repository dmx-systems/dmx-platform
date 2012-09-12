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
        storeConfigTopic(configTopic, null);    // ### FIXME: clientState=null. addConfigTopic() is not uesd anyway.
    }

    @Override
    public void addSetting(String configTypeUri, String settingUri, Object value) {
        // update memory
        boolean configTopicCreated = model.addSetting(configTypeUri, settingUri, value);
        // update DB
        storeSetting(configTopicCreated, configTypeUri, settingUri, value);
    }

    // ---

    @Override
    public ViewConfigurationModel getModel() {
        return model;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void store(ClientState clientState) {
        try {
            for (TopicModel configTopic : getConfigTopics()) {
                storeConfigTopic(configTopic, clientState);
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration failed (configurable=" + configurable + ")", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    private void storeConfigTopic(TopicModel configTopic, ClientState clientState) {
        Topic topic = dms.createTopic(configTopic, clientState);
        dms.createAssociation("dm4.core.aggregation", configurable,
            new TopicRoleModel(topic.getId(), "dm4.core.view_config"));
    }

    private void storeSetting(boolean configTopicCreated, String configTypeUri, String settingUri, Object value) {
        TopicModel configTopic = getConfigTopic(configTypeUri);
        if (configTopicCreated) {
            // Note: null is passed as clientState.
            // addSetting() is called from a migration and in a migration we have no clientState anyway.
            storeConfigTopic(configTopic, null);
        } else {
            Topic topic = new AttachedTopic(configTopic, dms);
            topic.setChildTopicValue(settingUri, new SimpleValue(value));
        }
    }
}
