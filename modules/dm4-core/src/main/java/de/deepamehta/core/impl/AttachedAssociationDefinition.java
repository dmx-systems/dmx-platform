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
    public String getWholeTypeUri() {
        return getModel().getWholeTypeUri();
    }

    @Override
    public String getPartTypeUri() {
        return getModel().getPartTypeUri();
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
        dms.objectFactory.storeWholeCardinalityUri(getId(), wholeCardinalityUri);
    }

    @Override
    public void setPartCardinalityUri(String partCardinalityUri, ClientState clientState, Directives directives) {
        // update memory
        getModel().setPartCardinalityUri(partCardinalityUri);
        // update DB
        dms.objectFactory.storePartCardinalityUri(getId(), partCardinalityUri);
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



    // === Attached Object Cache ===

    private void initViewConfig() {
        RoleModel configurable = dms.objectFactory.createConfigurableAssocDef(getId());   // ### ID is uninitialized
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
