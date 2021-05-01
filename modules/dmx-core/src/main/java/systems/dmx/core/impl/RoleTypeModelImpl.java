package systems.dmx.core.impl;

import systems.dmx.core.model.RoleTypeModel;
import systems.dmx.core.model.ViewConfigModel;

import org.codehaus.jettison.json.JSONObject;



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

    // TopicModel Overrides

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON()
                .put("viewConfigTopics", viewConfig.toJSONArray());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
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
