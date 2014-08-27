package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
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

    @Override
    public String getParentTypeUri() {
        return getModel().getParentTypeUri();
    }

    @Override
    public String getChildTypeUri() {
        return getModel().getChildTypeUri();
    }

    @Override
    public String getParentCardinalityUri() {
        return getModel().getParentCardinalityUri();
    }

    @Override
    public String getChildCardinalityUri() {
        return getModel().getChildCardinalityUri();
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
    public void setParentCardinalityUri(String parentCardinalityUri, Directives directives) {
        // update memory
        getModel().setParentCardinalityUri(parentCardinalityUri);
        // update DB
        dms.typeStorage.storeParentCardinalityUri(getId(), parentCardinalityUri);
    }

    @Override
    public void setChildCardinalityUri(String childCardinalityUri, Directives directives) {
        // update memory
        getModel().setChildCardinalityUri(childCardinalityUri);
        // update DB
        dms.typeStorage.storeChildCardinalityUri(getId(), childCardinalityUri);
    }



    // === Updating ===

    @Override
    public void update(AssociationDefinitionModel newModel, Directives directives) {
        // assoc type
        updateAssocTypeUri(newModel, directives);
        // cardinality
        updateParentCardinality(newModel.getParentCardinalityUri(), directives);
        updateChildCardinality(newModel.getChildCardinalityUri(), directives);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void updateAssocTypeUri(AssociationDefinitionModel newModel, Directives directives) {
        String newTypeUri = newModel.getTypeUri();
        // abort if no update is requested
        if (newTypeUri == null) {
            return;
        }
        //
        String typeUri = getTypeUri();
        if (!typeUri.equals(newTypeUri)) {
            super.update(newModel, directives);
        }
    }

    // ---

    private void updateParentCardinality(String newParentCardinalityUri, Directives directives) {
        // abort if no update is requested
        if (newParentCardinalityUri == null) {
            return;
        }
        //
        String parentCardinalityUri = getParentCardinalityUri();
        if (!parentCardinalityUri.equals(newParentCardinalityUri)) {
            logger.info("### Changing parent cardinality URI from \"" + parentCardinalityUri + "\" -> \"" +
                newParentCardinalityUri + "\"");
            setParentCardinalityUri(newParentCardinalityUri, directives);
        }
    }

    private void updateChildCardinality(String newChildCardinalityUri, Directives directives) {
        // abort if no update is requested
        if (newChildCardinalityUri == null) {
            return;
        }
        //
        String childCardinalityUri = getChildCardinalityUri();
        if (!childCardinalityUri.equals(newChildCardinalityUri)) {
            logger.info("### Changing child cardinality URI from \"" + childCardinalityUri + "\" -> \"" +
                newChildCardinalityUri + "\"");
            setChildCardinalityUri(newChildCardinalityUri, directives);
        }
    }



    // === Attached Object Cache ===

    private void initViewConfig() {
        RoleModel configurable = dms.typeStorage.createConfigurableAssocDef(getId());   // ### ID is uninitialized
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
