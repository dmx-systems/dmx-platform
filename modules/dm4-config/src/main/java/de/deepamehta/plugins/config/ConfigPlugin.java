package de.deepamehta.plugins.config;

import de.deepamehta.plugins.config.service.ConfigService;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Transactional;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.util.HashMap;
import java.util.Map;



@Path("/config")
@Produces("application/json")
public class ConfigPlugin extends PluginActivator implements ConfigService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String ASSOC_TYPE_CONFIGURATION = "dm4.config.configuration";
    private static String ROLE_TYPE_CONFIGURABLE = "dm4.config.configurable";
    private static String ROLE_TYPE_DEFAULT = "dm4.core.default";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, ConfigDefinition> configDefs = new HashMap();

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** ConfigService Implementation ***
    // ************************************



    @GET
    @Override    
    public ConfigDefinitions getConfigDefinitions() {
        return new ConfigDefinitions();
    }

    @GET
    @Path("/{config_type_uri}/topic/{topic_id}")
    @Transactional
    @Override    
    public RelatedTopic getConfigTopic(@PathParam("topic_id") long topicId,
                                       @PathParam("config_type_uri") String configTypeUri) {
        RelatedTopic configTopic = _getConfigTopic(topicId, configTypeUri);
        //
        if (configTopic == null) {
            configTopic = createConfigTopic(topicId, configTypeUri);
        }
        //
        return configTopic;
    }

    // ---

    @Override
    public void registerConfigDefinition(ConfigDefinition configDef) {
        try {
            String configTypeUri = configDef.getConfigTypeUri();
            //
            ConfigDefinition _configDef = getConfigDefinition(configTypeUri);
            if (_configDef != null) {
                throw new RuntimeException("A definition for configuration type \"" + configTypeUri +
                    "\" is already registered");
            }
            //
            configDefs.put(configTypeUri, configDef);
        } catch (Exception e) {
            throw new RuntimeException("Registering a configuration definition failed", e);
        }
    }

    @Override
    public void unregisterConfigDefinition(String configTypeUri) {
        try {
            ConfigDefinition _configDef = configDefs.remove(configTypeUri);
            //
            if (_configDef == null) {
                throw new RuntimeException("Definition for configuration type \"" + configTypeUri +
                    "\" not registered");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unregistering definition for configuration type \"" + configTypeUri +
                "\" failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private RelatedTopic _getConfigTopic(long topicId, String configTypeUri) {
        return dms.getTopic(topicId).getRelatedTopic(ASSOC_TYPE_CONFIGURATION, ROLE_TYPE_CONFIGURABLE,
            ROLE_TYPE_DEFAULT, configTypeUri);
    }

    private RelatedTopic createConfigTopic(long topicId, String configTypeUri) {
        Topic configTopic = dms.createTopic(getConfigDefinitionOrThrow(configTypeUri).getDefaultConfigTopic());
        dms.createAssociation(new AssociationModel(ASSOC_TYPE_CONFIGURATION,
            new TopicRoleModel(topicId, ROLE_TYPE_CONFIGURABLE),
            new TopicRoleModel(configTopic.getId(), ROLE_TYPE_DEFAULT)));
        // ### TODO: extend Core API to avoid re-retrieval
        return _getConfigTopic(topicId, configTypeUri);
    }

    // ---

    private ConfigDefinition getConfigDefinition(String configTypeUri) {
        return configDefs.get(configTypeUri);
    }

    private ConfigDefinition getConfigDefinitionOrThrow(String configTypeUri) {
        ConfigDefinition configDef = getConfigDefinition(configTypeUri);
        //
        if (configDef == null) {
            throw new RuntimeException("No configuration definition for type \"" + configTypeUri + "\" registered");
        }
        //
        return configDef;
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
