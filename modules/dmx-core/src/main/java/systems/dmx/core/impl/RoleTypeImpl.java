package systems.dmx.core.impl;

import systems.dmx.core.RoleType;
import systems.dmx.core.ViewConfig;
import systems.dmx.core.model.PlayerModel;



class RoleTypeImpl extends TopicImpl implements RoleType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    RoleTypeImpl(TopicModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // View Config

    @Override
    public final ViewConfig getViewConfig() {
        PlayerModel configurable = al.typeStorage.newTypePlayer(getId());
        return new ViewConfigImpl(configurable, getModel().getViewConfig(), al);
    }

    @Override
    public final Object getViewConfigValue(String configTypeUri, String childTypeUri) {
        return getModel().getViewConfigValue(configTypeUri, childTypeUri);
    }

    //

    @Override
    public RoleTypeModelImpl getModel() {
        return (RoleTypeModelImpl) model;
    }
}
