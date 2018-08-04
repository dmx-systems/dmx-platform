package systems.dmx.core.impl;

import systems.dmx.core.AssociationDefinition;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.RoleModel;



/**
 * An association definition that is attached to the {@link CoreService}.
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
        return new ViewConfigurationImpl(configurable, getModel().getViewConfig(), pl);
    }

    // ---

    @Override
    public void update(AssociationDefinitionModel updateModel) {
        model.update((AssociationDefinitionModelImpl) updateModel);     // ### FIXME: call through pl for access control
    }

    // ---

    @Override
    public AssociationDefinitionModelImpl getModel() {
        return (AssociationDefinitionModelImpl) model;
    }
}
