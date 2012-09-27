package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.logging.Logger;



/**
 * An association definition that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationDefinition extends AttachedAssociation implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AttachedViewConfiguration viewConfig;   // attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationDefinition(AssociationDefinitionModel model, EmbeddedService dms) {
        super(model, dms);
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************************
    // *** AssociationDefinition Implementation ***
    // ********************************************



    @Override
    public String getInstanceLevelAssocTypeUri() {
        return getModel().getInstanceLevelAssocTypeUri();
    }

    // ### FIXME: wording should be "getWholeTypeUri".
    // Also association types have association definitions
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
    public AssociationDefinitionModel getModel() {
        return (AssociationDefinitionModel) super.getModel();
    }

    // ---

    @Override
    public void setWholeCardinalityUri(String wholeCardinalityUri, ClientState clientState, Directives directives) {
        // update memory
        getModel().setWholeCardinalityUri(wholeCardinalityUri);
        // update DB
        storeWholeCardinalityUri(directives);
    }

    @Override
    public void setPartCardinalityUri(String partCardinalityUri, ClientState clientState, Directives directives) {
        // update memory
        getModel().setPartCardinalityUri(partCardinalityUri);
        // update DB
        storePartCardinalityUri(directives);
    }

    // === Updating ===

    @Override
    public void update(AssociationDefinitionModel newModel, ClientState clientState, Directives directives) {
        // assoc type
        updateAssocTypeUri(newModel, clientState, directives);
        // cardinality
        updateWholeCardinality(newModel.getWholeCardinalityUri(), clientState, directives);
        updatePartCardinality(newModel.getPartCardinalityUri(), clientState, directives);
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
            associateWholeRoleType();
            associatePartRoleType();
            // cardinality
            associateWholeCardinality();
            associatePartCardinality();
            //
            storeViewConfig();
        } catch (Exception e) {
            // ### FIXME wording: "type" should be "topic type" or "association type"
            throw new RuntimeException("Storing association definition \"" + getUri() +
                "\" of type \"" + getWholeTopicTypeUri() + "\" failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Update ===

    private void updateAssocTypeUri(AssociationDefinitionModel newModel, ClientState clientState,
                                                                         Directives directives) {
        String newTypeUri = newModel.getTypeUri();
        if (newTypeUri == null) {
            return;
        }
        //
        String typeUri = getTypeUri();
        if (!typeUri.equals(newTypeUri)) {
            super.update(newModel, clientState, directives);
        }
    }

    // ---

    private void updateWholeCardinality(String newWholeCardinalityUri, ClientState clientState, Directives directives) {
        if (newWholeCardinalityUri == null) {
            return;
        }
        //
        String wholeCardinalityUri = getWholeCardinalityUri();
        if (!wholeCardinalityUri.equals(newWholeCardinalityUri)) {
            logger.info("### Changing whole cardinality URI from \"" + wholeCardinalityUri + "\" -> \"" +
                newWholeCardinalityUri + "\"");
            setWholeCardinalityUri(newWholeCardinalityUri, clientState, directives);
        }
    }

    private void updatePartCardinality(String newPartCardinalityUri, ClientState clientState, Directives directives) {
        if (newPartCardinalityUri == null) {
            return;
        }
        //
        String partCardinalityUri = getPartCardinalityUri();
        if (!partCardinalityUri.equals(newPartCardinalityUri)) {
            logger.info("### Changing part cardinality URI from \"" + partCardinalityUri + "\" -> \"" +
                newPartCardinalityUri + "\"");
            setPartCardinalityUri(newPartCardinalityUri, clientState, directives);
        }
    }

    // === Store ===

    private void storeWholeCardinalityUri(Directives directives) {
        // remove current assignment
        dms.getObjectFactory().fetchWholeCardinality(this).getAssociation().delete(directives);
        // create new assignment
        associateWholeCardinality();
    }    

    private void storePartCardinalityUri(Directives directives) {
        // remove current assignment
        dms.getObjectFactory().fetchPartCardinality(this).getAssociation().delete(directives);
        // create new assignment
        associatePartCardinality();
    }    

    // ---

    private void associateWholeCardinality() {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(getWholeCardinalityUri(), "dm4.core.whole_cardinality"),
            new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
    }

    private void associatePartCardinality() {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(getPartCardinalityUri(), "dm4.core.part_cardinality"),
            new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
    }

    // ---

    private void associateWholeRoleType() {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(getWholeRoleTypeUri(), "dm4.core.whole_role_type"),
            new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
    }

    private void associatePartRoleType() {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(getPartRoleTypeUri(), "dm4.core.part_role_type"),
            new AssociationRoleModel(getId(), "dm4.core.assoc_def"));
    }

    // ---

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
