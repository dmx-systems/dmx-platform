package systems.dmx.core.impl;

import systems.dmx.core.service.PluginInfo;
import org.codehaus.jettison.json.JSONObject;



class PluginInfoImpl implements PluginInfo {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject pluginInfo = new JSONObject();

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   pluginFile      file name or empty
     * @param   styleFile       file name or empty
     */
    PluginInfoImpl(String pluginUri, String pluginFile, String styleFile) {
        try {
            pluginInfo.put("pluginUri", pluginUri);
            pluginInfo.put("pluginFile", pluginFile);
            pluginInfo.put("styleFile", styleFile);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return pluginInfo;
    }
}
