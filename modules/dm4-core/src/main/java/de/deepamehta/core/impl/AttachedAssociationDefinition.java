package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
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
    public void setParentCardinalityUri(String wholeCardinalityUri, ClientState clientState, Directives directives) {
        // update memory
        getModel().setParentCardinalityUri(wholeCardinalityUri);
        // update DB
        dms.typeStorage.storeWholeCardinalityUri(getId(), wholeCardinalityUri);
    }

    @Override
    public void setChildCardinalityUri(String partCardinalityUri, ClientState clientState, Directives directives) {
        // update memory
        getModel().setChildCardinalityUri(partCardinalityUri);
        // update DB
        dms.typeStorage.storePartCardinalityUri(getId(), partCardinalityUri);
    }



    // === Updating ===

    @Override
    public void update(AssociationDefinitionModel newModel, ClientState clientState, Directives directives) {
        // assoc type
        updateAssocTypeUri(newModel, clientState, directives);
        // cardinality
        updateWholeCardinality(newModel.getParentCardinalityUri(), clientState, directives);
        updatePartCardinality(newModel.getChildCardinalityUri(), clientState, directives);
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
        String wholeCardinalityUri = getParentCardinalityUri();
        if (!wholeCardinalityUri.equals(newWholeCardinalityUri)) {
            logger.info("### Changing whole cardinality URI from \"" + wholeCardinalityUri + "\" -> \"" +
                newWholeCardinalityUri + "\"");
            setParentCardinalityUri(newWholeCardinalityUri, clientState, directives);
        }
    }

    private void updatePartCardinality(String newPartCardinalityUri, ClientState clientState, Directives directives) {
        if (newPartCardinalityUri == null) {
            return;
        }
        //
        String partCardinalityUri = getChildCardinalityUri();
        if (!partCardinalityUri.equals(newPartCardinalityUri)) {
            logger.info("### Changing part cardinality URI from \"" + partCardinalityUri + "\" -> \"" +
                newPartCardinalityUri + "\"");
            setChildCardinalityUri(newPartCardinalityUri, clientState, directives);
        }
    }



    // === Attached Object Cache ===

    private void initViewConfig() {
        RoleModel configurable = dms.typeStorage.createConfigurableAssocDef(getId());   // ### ID is uninitialized
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
