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
class AssociationDefinitionImpl extends AssociationImpl implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TypeImpl parentType;

    private ViewConfigurationImpl viewConfig;   // attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationDefinitionImpl(AssociationDefinitionModel model, TypeImpl parentType, EmbeddedService dms) {
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

    @Override
    public AssociationDefinitionModelImpl getModel() {
        return (AssociationDefinitionModelImpl) super.getModel();
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
        try {
            boolean changeCustomAssocType = !getModel().hasSameCustomAssocType(newModel);
            if (changeCustomAssocType) {
                logger.info("### Changing custom association type URI from \"" + getCustomAssocTypeUri() +
                    "\" -> \"" + ((AssociationDefinitionModelImpl) newModel).getCustomAssocTypeUriOrNull() + "\"");
            }
            //
            super.update(newModel);
            //
            // cardinality
            updateParentCardinality(newModel.getParentCardinalityUri());
            updateChildCardinality(newModel.getChildCardinalityUri());
            //
            // rehash
            if (changeCustomAssocType) {
                String[] assocDefUris = parentType.getModel().findAssocDefUris(newModel.getId());
                parentType.rehashAssocDef(assocDefUris[0], assocDefUris[1]);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating association definition \"" + getAssocDefUri() +
                "\" failed (" + newModel + ")", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

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
        this.viewConfig = new ViewConfigurationImpl(configurable, getModel().getViewConfigModel(), dms);
    }
}
