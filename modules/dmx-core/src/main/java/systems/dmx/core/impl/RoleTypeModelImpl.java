package systems.dmx.core.impl;

import systems.dmx.core.model.RoleTypeModel;
import systems.dmx.core.model.ViewConfigModel;



class RoleTypeModelImpl extends TopicModelImpl implements RoleTypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    ViewConfigModelImpl viewConfig;     // is never null

    // ---------------------------------------------------------------------------------------------------- Constructors

    RoleTypeModelImpl(TopicModelImpl topic, ViewConfigModelImpl viewConfig) {
        super(topic);
        this.viewConfig = viewConfig;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // View Config

    @Override
    public ViewConfigModelImpl getViewConfig() {
        return viewConfig;
    }

    @Override
    public Object getViewConfigValue(String configTypeUri, String childTypeUri) {
        return viewConfig.getConfigValue(configTypeUri, childTypeUri);
    }

    @Override
    public void setViewConfig(ViewConfigModel viewConfig) {
        this.viewConfig = (ViewConfigModelImpl) viewConfig;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "role type";
    }

    @Override
    RoleTypeImpl instantiate() {
        return new RoleTypeImpl(this, al);
    }
}
