package de.deepamehta.core.impl.service;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import java.util.Set;



abstract class AttachedType extends AttachedTopic implements Type {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AttachedViewConfiguration viewConfig;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedType(EmbeddedService dms) {
        super((TopicModel) null, dms);  // The model and viewConfig remain uninitialized.
                                        // They are initialized later on through subtype's fetch().
    }

    AttachedType(TypeModel model, EmbeddedService dms) {
        super(model, dms);
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === Type Implementation ===

    @Override
    public AttachedViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // ---

    // FIXME: to be dropped
    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }

    // === AttachedTopic Overrides ===

    @Override
    public TypeModel getModel() {
        return (TypeModel) super.getModel();
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected final ViewConfigurationModel fetchViewConfig(Topic typeTopic) {
        Set<RelatedTopic> topics = typeTopic.getRelatedTopics("dm3.core.association", typeTopic.getTypeUri(),
            "dm3.core.view_config", null, true);    // fetchComposite=true
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(dms.getTopicModels(topics));
    }

    protected final void initViewConfig() {
        // Note: this type must be identified by its URI. Types being created have no ID yet.
        RoleModel configurable = new TopicRoleModel(getUri(), getTypeUri());
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
