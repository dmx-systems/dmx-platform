package systems.dmx.config;

import static systems.dmx.config.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.DirectivesResponse;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.service.event.PostCreateTopic;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/config")
@Produces("application/json")
public class ConfigPlugin extends PluginActivator implements ConfigService, PostCreateTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Key: the "configurable URI" as a config target's hash key, that is either "topicUri:{uri}" or "typeUri:{uri}".
     */
    private Map<String, List<ConfigDef>> registry = new HashMap();

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // --- ConfigService ---

    @GET
    @Override
    public ConfigDefs getConfigDefs() {
        try {
            JSONObject json = new JSONObject();
            PrivilegedAccess pa = dmx.getPrivilegedAccess();
            for (String configurableUri: registry.keySet()) {
                JSONArray array = new JSONArray();
                for (ConfigDef configDef : lookupConfigDefs(configurableUri)) {
                    String username = pa.getUsername(request);
                    long workspaceId = workspaceId(configDef.getConfigModRole());
                    if (pa.hasReadPermission(username, workspaceId)) {
                        array.put(configDef.getConfigTypeUri());
                    }
                }
                json.put(configurableUri, array);
            }
            return new ConfigDefs(json);
        } catch (Exception e) {
            throw new RuntimeException("Retrieving the registered config definitions failed", e);
        }
    }

    @GET
    @Path("/{configTypeUri}/topic/{topicId}")
    @Override
    public RelatedTopic getConfigTopic(@PathParam("configTypeUri") String configTypeUri,
                                       @PathParam("topicId") long topicId) {
        return _getConfigTopic(configTypeUri, topicId);
    }

    @PUT
    @Path("/topic/{topicId}")
    @Transactional
    @Override
    public DirectivesResponse updateConfigTopic(@PathParam("topicId") long topicId, TopicModel updateModel) {
        try {
            Topic topic = dmx.getTopic(topicId);
            String configTypeUri = updateModel.getTypeUri();
            RelatedTopic oldConfigTopic = getConfigTopic(configTypeUri, topicId);
            ConfigDef configDef = getApplicableConfigDef(topic, configTypeUri);
            long workspaceId = workspaceId(configDef.getConfigModRole());
            dmx.getPrivilegedAccess().runInWorkspaceContext(workspaceId, () -> {
                Topic configTopic = dmx.createTopic(updateModel);
                if (!configTopic.equals(oldConfigTopic)) {
                    logger.info("change " + oldConfigTopic.getId() + " -> " + configTopic.getId());
                    oldConfigTopic.getRelatingAssoc().delete();
                    createConfigAssoc(topic, configTopic);
                } else {
                    logger.info("no change");
                }
                return null;
            });
            return new DirectivesResponse();
        } catch (Exception e) {
            throw new RuntimeException("Updating config for topic " + topicId + " failed", e);
        }
    }

    @Override
    public void createConfigTopic(String configTypeUri, Topic topic) {
        _createConfigTopic(getApplicableConfigDef(topic, configTypeUri), topic);
    }

    // ---

    @Override
    public void registerConfigDef(ConfigDef configDef) {
        try {
            if (isRegistered(configDef)) {
                throw new RuntimeException("A definition for config type \"" + configDef.getConfigTypeUri() +
                    "\" is already registered");
            }
            //
            String hashKey = configDef.getHashKey();
            List<ConfigDef> configDefs = lookupConfigDefs(hashKey);
            if (configDefs == null) {
                configDefs = new ArrayList();
                registry.put(hashKey, configDefs);
            }
            configDefs.add(configDef);
        } catch (Exception e) {
            throw new RuntimeException("Registering a config definition failed", e);
        }
    }

    @Override
    public void unregisterConfigDef(String configTypeUri) {
        try {
            for (List<ConfigDef> configDefs : registry.values()) {
                ConfigDef configDef = findByConfigTypeUri(configDefs, configTypeUri);
                if (configDef != null) {
                    if (!configDefs.remove(configDef)) {
                        throw new RuntimeException("Config definition could not be removed from registry");
                    }
                    return;
                }
            }
            throw new RuntimeException("No such config definition registered");
        } catch (Exception e) {
            throw new RuntimeException("Unregistering definition for config type \"" + configTypeUri + "\" failed", e);
        }
    }

    // --- Listeners ---

    @Override
    public void postCreateTopic(Topic topic) {
        for (ConfigDef configDef : getApplicableConfigDefs(topic)) {
            _createConfigTopic(configDef, topic);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private RelatedTopic _getConfigTopic(String configTypeUri, long topicId) {
        return dmx.getPrivilegedAccess().getConfigTopic(configTypeUri, topicId);
    }

    private RelatedTopic _createConfigTopic(final ConfigDef configDef, final Topic topic) {
        final String configTypeUri = configDef.getConfigTypeUri();
        try {
            logger.info("### Creating config topic of type \"" + configTypeUri + "\" for topic " + topic.getId());
            final PrivilegedAccess pa = dmx.getPrivilegedAccess();
            // Note: a config topic requires a special workspace assignment.
            long workspaceId = workspaceId(configDef.getConfigModRole());
            return pa.runInWorkspaceContext(workspaceId, () -> {
                Topic configTopic = dmx.createTopic(configDef.getConfigValue(topic));
                createConfigAssoc(topic, configTopic);
                // ### TODO: extend Core API to avoid re-retrieval
                return _getConfigTopic(configTypeUri, topic.getId());
            });
        } catch (Exception e) {
            throw new RuntimeException("Creating config topic of type \"" + configTypeUri + "\" for topic " +
                topic.getId() + " failed", e);
        }
    }

    private void createConfigAssoc(Topic topic, Topic configTopic) {
        dmx.createAssoc(mf.newAssocModel(CONFIGURATION,
            mf.newTopicPlayerModel(topic.getId(), CONFIGURABLE),
            mf.newTopicPlayerModel(configTopic.getId(), DEFAULT)
        ));
    }

    private long workspaceId(ConfigModRole role) {
        PrivilegedAccess pa = dmx.getPrivilegedAccess();
        switch (role) {
        case ADMIN:
            return pa.getAdminWorkspaceId();
        case SYSTEM:
            return pa.getSystemWorkspaceId();
        default:
            throw new RuntimeException("Modification role \"" + role + "\" not yet implemented");
        }
    }

    // ---

    /**
     * Returns all config definitions applicable to a given topic.
     *
     * @return  a list of config definitions, possibly empty.
     */
    private List<ConfigDef> getApplicableConfigDefs(Topic topic) {
        List<ConfigDef> configDefs1 = lookupConfigDefs(ConfigTarget.SINGLETON.hashKey(topic));
        List<ConfigDef> configDefs2 = lookupConfigDefs(ConfigTarget.TYPE_INSTANCES.hashKey(topic));
        if (configDefs1 != null && configDefs2 != null) {
            List<ConfigDef> configDefs = new ArrayList();
            configDefs.addAll(configDefs1);
            configDefs.addAll(configDefs2);
            return configDefs;
        }
        return configDefs1 != null ? configDefs1 : configDefs2 != null ? configDefs2 : new ArrayList();
    }

    /**
     * Returns the config definition for the given config type that is applicable to the given topic.
     *
     * @throws RuntimeException     if no such config definition is registered.
     */
    private ConfigDef getApplicableConfigDef(Topic topic, String configTypeUri) {
        List<ConfigDef> configDefs = getApplicableConfigDefs(topic);
        if (configDefs.size() == 0) {
            throw new RuntimeException("None of the registered config definitions are applicable to " + info(topic));
        }
        ConfigDef configDef = findByConfigTypeUri(configDefs, configTypeUri);
        if (configDef == null) {
            throw new RuntimeException("For " + info(topic) + " no config definition for type \"" + configTypeUri +
                "\" registered");
        }
        return configDef;
    }

    // ---

    private boolean isRegistered(ConfigDef configDef) {
        for (List<ConfigDef> configDefs : registry.values()) {
            if (configDefs.contains(configDef)) {
                return true;
            }
        }
        return false;
    }

    private ConfigDef findByConfigTypeUri(List<ConfigDef> configDefs, String configTypeUri) {
        for (ConfigDef configDef : configDefs) {
            if (configDef.getConfigTypeUri().equals(configTypeUri)) {
                return configDef;
            }
        }
        return null;
    }

    private List<ConfigDef> lookupConfigDefs(String hashKey) {
        return registry.get(hashKey);
    }

    // ---

    private String info(Topic topic) {
        return "topic " + topic.getId() + " (value=\"" + topic.getSimpleValue() + "\", typeUri=\"" +
            topic.getTypeUri() + "\", uri=\"" + topic.getUri() + "\")";
    }
}
