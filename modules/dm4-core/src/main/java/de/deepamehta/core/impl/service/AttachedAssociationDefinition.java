package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;
import java.util.logging.Logger;



/**
 * An association definition that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationDefinition extends AttachedAssociation implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AttachedViewConfiguration viewConfig;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationDefinition(AssociationDefinitionModel model, EmbeddedService dms) {
        super(model, dms);
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssociationDefinition Implementation ===

    @Override
    public String getInstanceLevelAssocTypeUri() {
        return getModel().getInstanceLevelAssocTypeUri();
    }

    @Override
    public String getWholeTopicTypeUri() {
        return getModel().getWholeTopicTypeUri();
    }

    @Override
    public String getPartTopicTypeUri() {
        return getModel().getPartTopicTypeUri();
    }

    @Override
    public String getWholeRoleTypeUri() {
        return getModel().getWholeRoleTypeUri();
    }

    @Override
    public String getPartRoleTypeUri() {
        return getModel().getPartRoleTypeUri();
    }

    @Override
    public String getWholeCardinalityUri() {
        return getModel().getWholeCardinalityUri();
    }

    @Override
    public String getPartCardinalityUri() {
        return getModel().getPartCardinalityUri();
    }

    @Override
    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // ---

    @Override
    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        getModel().setWholeCardinalityUri(wholeCardinalityUri);
    }

    @Override
    public void setPartCardinalityUri(String partCardinalityUri) {
        getModel().setPartCardinalityUri(partCardinalityUri);
    }

    // === Updating ===

    @Override
    public void update(AssociationDefinitionModel model, ClientState clientState, Directives directives) {
        updateAssocTypeUri(model, clientState, directives);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void store() {
        try {
            // Note: creating the underlying association is conditional. It exists already for
            // an interactively created association definition. Its ID is already set.
            if (getId() == -1) {
                dms.createAssociation(getModel(), null);    // clientState=null
            }
            // role types
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getWholeRoleTypeUri(), "dm4.core.whole_role_type"),
                new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getPartRoleTypeUri(), "dm4.core.part_role_type"),
                new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
            // cardinality
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getWholeCardinalityUri(), "dm4.core.whole_cardinality"),
                new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getPartCardinalityUri(), "dm4.core.part_cardinality"),
                new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
            //
            storeViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + getUri() +
                "\" of topic type \"" + getWholeTopicTypeUri() + "\" failed", e);
        }
    }

    // ---

    @Override
    public AssociationDefinitionModel getModel() {
        return (AssociationDefinitionModel) super.getModel();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void updateAssocTypeUri(AssociationDefinitionModel model, ClientState clientState, Directives directives) {
        String newTypeUri = model.getTypeUri();
        if (newTypeUri == null) {
            return;
        }
        String typeUri = getTypeUri();
        if (!typeUri.equals(newTypeUri)) {
            super.update(model, clientState, directives);
        }
    }



    // === Store ===

    private void storeViewConfig() {
        try {
            for (TopicModel configTopic : getModel().getViewConfigModel().getConfigTopics()) {
                Topic topic = dms.createTopic(configTopic, null);   // FIXME: clientState=null
                dms.createAssociation("dm4.core.aggregation",
                    new AssociationRoleModel(getId(), "dm4.core.assoc_def"),
                    new TopicRoleModel(topic.getId(), "dm4.core.view_config"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration of association definition \"" +
                getUri() + "\" failed", e);
        }
    }



    // === Helper ===

    private void initViewConfig() {
        RoleModel configurable = new AssociationRoleModel(getId(), "dm4.core.assoc_def");
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
