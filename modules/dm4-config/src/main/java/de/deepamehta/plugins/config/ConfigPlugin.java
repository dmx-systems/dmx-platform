package de.deepamehta.plugins.config;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.plugins.config.service.ConfigService;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.util.HashMap;
import java.util.Map;



@Path("/config")
@Produces("application/json")
public class ConfigPlugin extends PluginActivator implements ConfigService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, ConfigDefinition> configDefs = new HashMap();

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** ConfigService Implementation ***
    // ************************************



    @GET
    @Path("/defs")
    @Override    
    public ConfigDefinitions getConfigDefinitions() {
        return new ConfigDefinitions();
    }

    @Override
    public void registerConfigDefinition(ConfigDefinition configDef) {
        try {
            String configTypeUri = configDef.getConfigTypeUri();
            //
            ConfigDefinition _configDef = configDefs.get(configTypeUri);
            if (_configDef != null) {
                throw new RuntimeException("Definition for configuration type \"" + configTypeUri + "\" already exits");
            }
            //
            configDefs.put(configTypeUri, configDef);
        } catch (Exception e) {
            throw new RuntimeException("Registering a configuration definition failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    public class ConfigDefinitions implements JSONEnabled {

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject o = new JSONObject();
                for (ConfigDefinition configDef : configDefs.values()) {
                    String configurableUri = configDef.getConfigurableUri();
                    JSONArray array = o.optJSONArray(configurableUri);
                    if (array == null) {
                        array = new JSONArray();
                        o.put(configurableUri, array);
                    }
                    array.put(configDef.getConfigTypeUri());
                }
                return o;
            } catch (Exception e) {
                throw new RuntimeException("Serialization failed (" + this + ")", e);
            }
        }
    }
}
