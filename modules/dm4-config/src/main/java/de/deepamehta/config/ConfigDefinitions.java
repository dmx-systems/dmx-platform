package de.deepamehta.config;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;
import java.util.Map;



class ConfigDefinitions implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, List<ConfigDefinition>> registry;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ConfigDefinitions(Map<String, List<ConfigDefinition>> registry) {
        this.registry = registry;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            for (String configurableUri: registry.keySet()) {
                List<ConfigDefinition> configDefs = registry.get(configurableUri);
                JSONArray array = new JSONArray();
                o.put(configurableUri, array);
                for (ConfigDefinition configDef : configDefs) {
                    array.put(configDef.getConfigTypeUri());
                }
            }
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
