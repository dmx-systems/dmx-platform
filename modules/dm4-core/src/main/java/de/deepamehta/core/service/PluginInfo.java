package de.deepamehta.core.service;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

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
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    public static JSONArray pluginInfoToJson(Set<PluginInfo> pluginInfoSet) {
        JSONArray array = new JSONArray();
        for (PluginInfo pluginInfo : pluginInfoSet) {
            array.put(pluginInfo.toJSON());
        }
        return array;
    }
}
