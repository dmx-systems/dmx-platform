package de.deepamehta.plugins.config;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.service.event.PostCreateTopicListener;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



@Path("/config")
@Produces("application/json")
public class ConfigPlugin extends PluginActivator implements ConfigService, PostCreateTopicListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String ASSOC_TYPE_CONFIGURATION = "dm4.config.configuration";
    private static String ROLE_TYPE_CONFIGURABLE = "dm4.config.configurable";
    private static String ROLE_TYPE_DEFAULT = "dm4.core.default";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Key: the "configurable URI" as a config target's hash key, that is either "topic_uri:{uri}" or "type_uri:{uri}".
     */
    private Map<String, List<ConfigDefinition>> registry = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** ConfigService Implementation ***
    // ************************************



    @GET
    @Path("/{config_type_uri}/topic/{topic_id}")
    @Override    
    public RelatedTopic getConfigTopic(@PathParam("config_type_uri") String configTypeUri,
                                       @PathParam("topic_id") long topicId) {
        return _getConfigTopic(configTypeUri, topicId);
    }

    @Override
    public void createConfigTopic(String configTypeUri, Topic topic) {
        createConfigTopic(getApplicableConfigDefinition(topic, configTypeUri), topic);
    }

    // ---

    @Override
    public void registerConfigDefinition(ConfigDefinition configDef) {
        try {
            if (isRegistered(configDef)) {
                throw new RuntimeException("A definition for configuration type \"" + configDef.getConfigTypeUri() +
                    "\" is already registered");
            }
            //
            String hashKey = configDef.getHashKey();
            List<ConfigDefinition> configDefs = lookupConfigDefinitions(hashKey);
            if (configDefs == null) {
                configDefs = new ArrayList();
                registry.put(hashKey, configDefs);
            }
            configDefs.add(configDef);
        } catch (Exception e) {
            throw new RuntimeException("Registering a configuration definition failed", e);
        }
    }

    @Override
    public void unregisterConfigDefinition(String configTypeUri) {
        try {
            for (List<ConfigDefinition> configDefs : registry.values()) {
                ConfigDefinition configDef = findByConfigTypeUri(configDefs, configTypeUri);
                if (configDef != null) {
                    if (!configDefs.remove(configDef)) {
                        throw new RuntimeException("Configuration definition could not be removed from registry");
                    }
                    return;
                }
            }
            throw new RuntimeException("No such configuration definition registered");
        } catch (Exception e) {
            throw new RuntimeException("Unregistering definition for configuration type \"" + configTypeUri +
                "\" failed", e);
        }
    }

    // --- not part of OSGi service ---

    @GET
    public ConfigDefinitions getConfigDefinitions() {
        return new ConfigDefinitions(registry);
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postCreateTopic(Topic topic) {
        for (ConfigDefinition configDef : getApplicableConfigDefinitions(topic)) {
            createConfigTopic(configDef, topic);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private RelatedTopic _getConfigTopic(String configTypeUri, long topicId) {
        return dms.getAccessControl().getConfigTopic(configTypeUri, topicId);
    }

    private RelatedTopic createConfigTopic(final ConfigDefinition configDef, final Topic topic) {
        final String configTypeUri = configDef.getConfigTypeUri();
        try {
            logger.info("### Creating config topic of type \"" + configTypeUri + "\" for topic " + topic.getId());
            // We suppress standard workspace assignment here as a config topic requires a special assignment.
            // See assignConfigTopicToWorkspace() below.
            return dms.getAccessControl().runWithoutWorkspaceAssignment(new Callable<RelatedTopic>() {
                @Override
                public RelatedTopic call() {
                    Topic configTopic = dms.createTopic(configDef.getConfigValue(topic));
                    dms.createAssociation(new AssociationModel(ASSOC_TYPE_CONFIGURATION,
                        new TopicRoleModel(topic.getId(), ROLE_TYPE_CONFIGURABLE),
                        new TopicRoleModel(configTopic.getId(), ROLE_TYPE_DEFAULT)));
                    assignConfigTopicToWorkspace(configTopic, configDef.getConfigModificationRole());
                    // ### TODO: extend Core API to avoid re-retrieval
                    return _getConfigTopic(configTypeUri, topic.getId());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Creating config topic of type \"" + configTypeUri + "\" for topic " +
                topic.getId() + " failed", e);
        }
    }

    private void assignConfigTopicToWorkspace(Topic configTopic, ConfigModificationRole role) {
        long workspaceId;
        AccessControl ac = dms.getAccessControl();
        switch (role) {
        case ADMIN:
            workspaceId = ac.getAdministrationWorkspaceId();
            break;
        case SYSTEM:
            workspaceId = ac.getSystemWorkspaceId();
            break;
        default:
            throw new RuntimeException("Modification role \"" + role + "\" not yet implemented");
        }
        ac.assignToWorkspace(configTopic, workspaceId);
    }

    // ---

    /**
     * Returns all configuration definitions applicable to a given topic.
     *
     * @return  a list of configuration definitions, possibly empty.
     */
    private List<ConfigDefinition> getApplicableConfigDefinitions(Topic topic) {
        List<ConfigDefinition> configDefs1 = lookupConfigDefinitions(ConfigTarget.SINGLETON.hashKey(topic));
        List<ConfigDefinition> configDefs2 = lookupConfigDefinitions(ConfigTarget.TYPE_INSTANCES.hashKey(topic));
        if (configDefs1 != null && configDefs2 != null) {
            List<ConfigDefinition> configDefs = new ArrayList();
            configDefs.addAll(configDefs1);
            configDefs.addAll(configDefs2);
            return configDefs;
        }
        return configDefs1 != null ? configDefs1 : configDefs2 != null ? configDefs2 : new ArrayList();
    }

    /**
     * Returns the configuration definition for the given config type that is applicable to the given topic.
     *
     * @throws RuntimeException     if no such configuration definition is registered.
     */
    private ConfigDefinition getApplicableConfigDefinition(Topic topic, String configTypeUri) {
        List<ConfigDefinition> configDefs = getApplicableConfigDefinitions(topic);
        if (configDefs.size() == 0) {
            throw new RuntimeException("None of the registered configuration definitions are applicable to " +
                info(topic));
        }
        ConfigDefinition configDef = findByConfigTypeUri(configDefs, configTypeUri);
        if (configDef == null) {
            throw new RuntimeException("For " + info(topic) + " no configuration definition for type \"" +
                configTypeUri + "\" registered");
        }
        return configDef;
    }

    // ---

    private boolean isRegistered(ConfigDefinition configDef) {
        for (List<ConfigDefinition> configDefs : registry.values()) {
            if (configDefs.contains(configDef)) {
                return true;
            }
        }
        return false;
    }

    private ConfigDefinition findByConfigTypeUri(List<ConfigDefinition> configDefs, String configTypeUri) {
        for (ConfigDefinition configDef : configDefs) {
            if (configDef.getConfigTypeUri().equals(configTypeUri)) {
                return configDef;
            }
        }
        return null;
    }

    private List<ConfigDefinition> lookupConfigDefinitions(String hashKey) {
        return registry.get(hashKey);
    }

    // ---

    private String info(Topic topic) {
        return "topic " + topic.getId() + " (value=\"" + topic.getSimpleValue() + "\", typeUri=\"" +
            topic.getTypeUri() + "\", uri=\"" + topic.getUri() + "\")";
    }
}
