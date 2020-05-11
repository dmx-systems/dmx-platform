package systems.dmx.core.impl;

import systems.dmx.core.CompDef;
import systems.dmx.core.ViewConfig;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.PlayerModel;



/**
 * A comp def that is attached to the {@link CoreService}.
 */
class CompDefImpl extends AssocImpl implements CompDef {

    // ---------------------------------------------------------------------------------------------------- Constructors

    CompDefImpl(CompDefModelImpl model, AccessLayer al) {
        super(model, al);
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
    public ViewConfig getViewConfig() {
        PlayerModel configurable = al.typeStorage.newCompDefPlayer(getId());   // ### ID is uninitialized
        return new ViewConfigImpl(configurable, getModel().getViewConfig(), al);
    }

    // ---

    @Override
    public void update(CompDefModel updateModel) {
        model.update((CompDefModelImpl) updateModel);     // ### FIXME: call through al for access control
    }

    // ---

    @Override
    public CompDefModelImpl getModel() {
        return (CompDefModelImpl) model;
    }
}
