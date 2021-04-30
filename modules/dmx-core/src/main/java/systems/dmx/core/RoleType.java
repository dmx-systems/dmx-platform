package systems.dmx.core;

import systems.dmx.core.model.RoleTypeModel;



/**
 * A role type is basically a topic plus a view config.
 */
public interface RoleType extends Topic {

    // View Config

    ViewConfig getViewConfig();

    Object getViewConfigValue(String configTypeUri, String childTypeUri);

    //

    RoleTypeModel getModel();
}
