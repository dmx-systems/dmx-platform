package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RoleModel;



/**
 * An association definition that is attached to the {@link DeepaMehtaService}.
 */
class AssociationDefinitionImpl extends AssociationImpl implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationDefinitionImpl(AssociationDefinitionModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************************
    // *** AssociationDefinition Implementation ***
    // ********************************************



    @Override
    public String getAssocDefUri() {
        return getModel().getAssocDefUri();
    }

    // ---

    @Override
    public String getParentTypeUri() {
        return getModel().getParentTypeUri();
    }

    @Override
    public String getChildTypeUri() {
        return getModel().getChildTypeUri();
    }

    // ---

    @Override
    public String getCustomAssocTypeUri() {
        return getModel().getCustomAssocTypeUri();
    }

    @Override
    public String getInstanceLevelAssocTypeUri() {
        return getModel().getInstanceLevelAssocTypeUri();
    }

    // --- Parent Cardinality ---

    @Override
    public String getParentCardinalityUri() {
        return getModel().getParentCardinalityUri();
    }

    @Override
    public void setParentCardinalityUri(String parentCardinalityUri) {
        getModel().updateParentCardinalityUri(parentCardinalityUri);
    }

    // --- Child Cardinality ---

    @Override
    public String getChildCardinalityUri() {
        return getModel().getChildCardinalityUri();
    }

    @Override
    public void setChildCardinalityUri(String childCardinalityUri) {
        getModel().updateChildCardinalityUri(childCardinalityUri);
    }

    // ---

    @Override
    public ViewConfiguration getViewConfig() {
        RoleModel configurable = pl.typeStorage.newAssocDefRole(getId());   // ### ID is uninitialized
        return new ViewConfigurationImpl(configurable, getModel().getViewConfigModel(), pl);
    }

    // ---

    @Override
    public void update(AssociationDefinitionModel newModel) {
        model.update(newModel);
    }

    // ---

    @Override
    public AssociationDefinitionModelImpl getModel() {
        return (AssociationDefinitionModelImpl) model;
    }
}
