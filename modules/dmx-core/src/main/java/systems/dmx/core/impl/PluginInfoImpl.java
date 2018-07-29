package systems.dmx.core.impl;

import systems.dmx.core.service.PluginInfo;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.Bundle;



class PluginInfoImpl implements PluginInfo {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_FILE = "/web/plugin.js";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Bundle pluginBundle;
    private JSONObject pluginInfo = new JSONObject();

    // ---------------------------------------------------------------------------------------------------- Constructors

    PluginInfoImpl(String pluginUri, Bundle pluginBundle) {
        this.pluginBundle = pluginBundle;
        try {
            pluginInfo.put("pluginUri", pluginUri);
            pluginInfo.put("hasPluginFile", hasPluginFile());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return pluginInfo;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean hasPluginFile() {
        return pluginBundle.getEntry(PLUGIN_FILE) != null;
    }
}
