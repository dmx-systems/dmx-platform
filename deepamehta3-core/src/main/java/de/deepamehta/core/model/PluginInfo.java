package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class PluginInfo {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    String pluginId;
    String pluginFile;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public PluginInfo(String pluginId, String pluginFile) {
        this.pluginId = pluginId;
        this.pluginFile = pluginFile;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public JSONObject toJSON() {
        try {
            JSONObject pluginInfo = new JSONObject();
            pluginInfo.put("plugin_id", pluginId);
            pluginInfo.put("plugin_file", pluginFile);
            return pluginInfo;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }
}
