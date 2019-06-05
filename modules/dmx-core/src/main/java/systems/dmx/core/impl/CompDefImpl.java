package systems.dmx.core.impl;

import systems.dmx.core.CompDef;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.PlayerModel;



/**
 * A comp def that is attached to the {@link CoreService}.
 */
class CompDefImpl extends AssocImpl implements CompDef {

    // ---------------------------------------------------------------------------------------------------- Constructors

    CompDefImpl(CompDefModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************
    // *** CompDef Implementation ***
    // ******************************



    @Override
    public String getCompDefUri() {
        return getModel().getCompDefUri();
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
        throw new UnsupportedOperationException();
    }

    // ---

    @Override
    public ViewConfiguration getViewConfig() {
        PlayerModel configurable = pl.typeStorage.newCompDefRole(getId());   // ### ID is uninitialized
        return new ViewConfigurationImpl(configurable, getModel().getViewConfig(), pl);
    }

    // ---

    @Override
    public void update(CompDefModel updateModel) {
        model.update((CompDefModelImpl) updateModel);     // ### FIXME: call through pl for access control
    }

    // ---

    @Override
    public CompDefModelImpl getModel() {
        return (CompDefModelImpl) model;
    }
}
