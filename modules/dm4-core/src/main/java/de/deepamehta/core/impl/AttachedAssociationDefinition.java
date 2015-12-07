package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;

import java.util.logging.Logger;



/**
 * An association definition that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationDefinition extends AttachedAssociation implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Type parentType;    // ### TODO: needed for rehashing while update?

    private AttachedViewConfiguration viewConfig;   // attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationDefinition(AssociationDefinitionModel model, Type parentType, EmbeddedService dms) {
        super(model, dms);
        this.parentType = parentType;
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************************
    // *** AssociationDefinition Implementation ***
    // ********************************************



    @Override
    public String getAssocDefUri() {
        return getModel().getAssocDefUri();
    }

    @Override
    public String getCustomAssocTypeUri() {
        return getModel().getCustomAssocTypeUri();
    }

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

    // ### TODO: drop this method
    @Override
    public void setCustomAssocTypeUri(String customAssocTypeUri) {
        logger.info("##################################### customAssocTypeUri=\"" + customAssocTypeUri + "\"");
        // Note: calling high-level methods lets the Type Editor kick in which is not intended. It refetches the
        // entire assoc def then and adds it to the type. This is more work than required, but is not really a problem.
        if (customAssocTypeUri != null) {
            getChildTopics().setRef("dm4.core.assoc_type#dm4.core.custom_assoc_type", customAssocTypeUri);
        } else {
            getChildTopics().setDeletionRef("dm4.core.assoc_type#dm4.core.custom_assoc_type", customAssocTypeUri);
        }
    }

    // ---

    @Override
    public void setParentCardinalityUri(String parentCardinalityUri) {
        // update memory
        getModel().setParentCardinalityUri(parentCardinalityUri);
        // update DB
        dms.typeStorage.storeParentCardinalityUri(getId(), parentCardinalityUri);
    }

    @Override
    public void setChildCardinalityUri(String childCardinalityUri) {
        // update memory
        getModel().setChildCardinalityUri(childCardinalityUri);
        // update DB
        dms.typeStorage.storeChildCardinalityUri(getId(), childCardinalityUri);
    }



    // === Updating ===

    @Override
    public void update(AssociationDefinitionModel newModel) {
        // assoc type
        updateAssocTypeUri(newModel);
        // custom assoc type
        updateCustomAssocTypeUri(newModel.getCustomAssocTypeUri());
        // cardinality
        updateParentCardinality(newModel.getParentCardinalityUri());
        updateChildCardinality(newModel.getChildCardinalityUri());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void updateAssocTypeUri(AssociationDefinitionModel newModel) {
        String newTypeUri = newModel.getTypeUri();
        // abort if no update is requested
        if (newTypeUri == null) {
            return;
        }
        //
        String typeUri = getTypeUri();
        if (!typeUri.equals(newTypeUri)) {
            super.update(newModel);
        }
    }

    private void updateCustomAssocTypeUri(String newCustomAssocTypeUri) {
        /* abort if no update is requested ### TODO
        if (newCustomAssocTypeUri == null) {
            return;
        } */
        //
        String customAssocTypeUri = getCustomAssocTypeUri();
        if (customAssocTypeUri != null ? !customAssocTypeUri.equals(newCustomAssocTypeUri) :
                                          newCustomAssocTypeUri != null) {
            logger.info("### Changing custom association type URI from \"" + customAssocTypeUri + "\" -> \"" +
                newCustomAssocTypeUri + "\"");
            setCustomAssocTypeUri(newCustomAssocTypeUri);
        }
    }

    // ---

    private void updateParentCardinality(String newParentCardinalityUri) {
        // abort if no update is requested
        if (newParentCardinalityUri == null) {
            return;
        }
        //
        String parentCardinalityUri = getParentCardinalityUri();
        if (!parentCardinalityUri.equals(newParentCardinalityUri)) {
            logger.info("### Changing parent cardinality URI from \"" + parentCardinalityUri + "\" -> \"" +
                newParentCardinalityUri + "\"");
            setParentCardinalityUri(newParentCardinalityUri);
        }
    }

    private void updateChildCardinality(String newChildCardinalityUri) {
        // abort if no update is requested
        if (newChildCardinalityUri == null) {
            return;
        }
        //
        String childCardinalityUri = getChildCardinalityUri();
        if (!childCardinalityUri.equals(newChildCardinalityUri)) {
            logger.info("### Changing child cardinality URI from \"" + childCardinalityUri + "\" -> \"" +
                newChildCardinalityUri + "\"");
            setChildCardinalityUri(newChildCardinalityUri);
        }
    }



    // === Attached Object Cache ===

    private void initViewConfig() {
        RoleModel configurable = dms.typeStorage.createConfigurableAssocDef(getId());   // ### ID is uninitialized
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
