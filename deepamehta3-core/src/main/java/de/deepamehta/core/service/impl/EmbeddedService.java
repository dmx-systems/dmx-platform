package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.AssociationRole;
import de.deepamehta.core.model.AssociationTypeData;
import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.CommandParams;
import de.deepamehta.core.model.CommandResult;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.MetaTypeData;
import de.deepamehta.core.model.PluginInfo;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicRole;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.ViewConfiguration;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.storage.DeepaMehtaTransaction;
import de.deepamehta.core.util.JSONHelper;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Implementation of the DeepaMehta core service. Embeddable into Java applications.
 */
@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class EmbeddedService implements CoreService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String CORE_MIGRATIONS_PACKAGE = "de.deepamehta.core.migrations";
    private static final int REQUIRED_CORE_MIGRATION = 3;

    // ---------------------------------------------------------------------------------------------- Instance Variables

            DeepaMehtaStorage storage;

    private PluginCache pluginCache = new PluginCache();

    private TypeCache typeCache;

    private enum Hook {

        // Note: this hook is triggered only by the plugin itself
        // (see {@link de.deepamehta.core.service.Plugin#initPlugin}).
        // It is declared here for documentation purpose only.
        POST_INSTALL_PLUGIN("postInstallPluginHook"),
        ALL_PLUGINS_READY("allPluginsReadyHook"),

        // Note: this hook is triggered only by the plugin itself
        // (see {@link de.deepamehta.core.service.Plugin#createServiceTracker}).
        // It is declared here for documentation purpose only.
        SERVICE_ARRIVED("serviceArrived", PluginService.class),
        // Note: this hook is triggered only by the plugin itself
        // (see {@link de.deepamehta.core.service.Plugin#createServiceTracker}).
        // It is declared here for documentation purpose only.
        SERVICE_GONE("serviceGone", PluginService.class),

         PRE_CREATE_TOPIC("preCreateHook",  TopicData.class, ClientContext.class),
        POST_CREATE_TOPIC("postCreateHook", Topic.class, ClientContext.class),
        // ### PRE_UPDATE_TOPIC("preUpdateHook",  Topic.class, Properties.class),
        // ### POST_UPDATE_TOPIC("postUpdateHook", Topic.class, Properties.class),

         PRE_DELETE_RELATION("preDeleteRelationHook",  Long.TYPE),
        POST_DELETE_RELATION("postDeleteRelationHook", Long.TYPE),

        PROVIDE_TOPIC_PROPERTIES("providePropertiesHook", Topic.class),
        PROVIDE_RELATION_PROPERTIES("providePropertiesHook", Association.class),

        ENRICH_TOPIC("enrichTopicHook", Topic.class, ClientContext.class),
        ENRICH_TOPIC_TYPE("enrichTopicTypeHook", TopicType.class, ClientContext.class),

        // Note: besides regular triggering (see {@link #createTopicType})
        // this hook is triggered by the plugin itself
        // (see {@link de.deepamehta.core.service.Plugin#introduceTypesToPlugin}).
        MODIFY_TOPIC_TYPE("modifyTopicTypeHook", TopicType.class, ClientContext.class),

        EXECUTE_COMMAND("executeCommandHook", String.class, CommandParams.class, ClientContext.class);

        private final String methodName;
        private final Class[] paramClasses;

        private Hook(String methodName, Class... paramClasses) {
            this.methodName = methodName;
            this.paramClasses = paramClasses;
        }
    }

    private enum MigrationRunMode {
        CLEAN_INSTALL, UPDATE, ALWAYS
    }

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public EmbeddedService(DeepaMehtaStorage storage) {
        this.storage = storage;
        this.typeCache = new TypeCache(this);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** CoreService Implementation ***
    // **********************************



    // === Topics ===

    @GET
    @Path("/topic/{id}")
    @Override
    public Topic getTopic(@PathParam("id") long id, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.getTopic(id);
            triggerHook(Hook.ENRICH_TOPIC, topic, clientContext);
            tx.success();
            return buildTopic(topic, true);
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic failed (id=" + id + ")", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic/by_property/{key}/{value}")
    @Override
    public Topic getTopic(@PathParam("key") String key, @PathParam("value") TopicValue value) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.getTopic(key, value);
            tx.success();
            return topic != null ? buildTopic(topic, true) : null;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic failed (key=\"" + key + "\", value=" + value + ")", e);
        } finally {
            tx.finish();
        }
    }

    /* @GET
    @Path("/topic/{typeUri}/{key}/{value}")
    @Override
    public Topic getTopic(@PathParam("typeUri") String typeUri,
                          @PathParam("key")     String key,
                          @PathParam("value")   TopicValue value) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.getTopic(typeUri, key, value);
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Error while retrieving topic (typeUri=\"" + typeUri + "\", " +
                "\"" + key + "\"=" + value + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public TopicValue getTopicProperty(long topicId, String key) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            TopicValue value = storage.getTopicProperty(topicId, key);
            tx.success();
            return value;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Property \"" + key + "\" of topic " + topicId + " can't be retrieved", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic/by_type/{typeUri}")
    @Override
    public List<Topic> getTopics(@PathParam("typeUri") String typeUri) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            List<Topic> topics = storage.getTopics(typeUri);
            //
            for (Topic topic : topics) {
                triggerHook(Hook.PROVIDE_TOPIC_PROPERTIES, topic);
            }
            //
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Topics of type \"" + typeUri + "\" can't be retrieved", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public List<Topic> getTopics(String key, Object value) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            List<Topic> topics = storage.getTopics(key, value);
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Error while retrieving topics by property (\"" + key + "\"=" + value + ")", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic/{id}/related_topics")
    @Override
    public List<RelatedTopic> getRelatedTopics(@PathParam("id") long topicId,
                                               @QueryParam("include_topic_types") List<String> includeTopicTypes,
                                               @QueryParam("include_rel_types")   List<String> includeRelTypes,
                                               @QueryParam("exclude_rel_types")   List<String> excludeRelTypes) {
        // set defaults
        if (includeTopicTypes == null) includeTopicTypes = new ArrayList();
        if (includeRelTypes   == null) includeRelTypes   = new ArrayList();
        if (excludeRelTypes   == null) excludeRelTypes   = new ArrayList();
        // error check
        if (!includeRelTypes.isEmpty() && !excludeRelTypes.isEmpty()) {
            throw new IllegalArgumentException("includeRelTypes and excludeRelTypes can not be used at the same time");
        }
        //
        DeepaMehtaTransaction tx = beginTx();
        try {
            List<RelatedTopic> relTopics = storage.getRelatedTopics(topicId, includeTopicTypes, includeRelTypes,
                                                                                                excludeRelTypes);
            //
            for (RelatedTopic relTopic : relTopics) {
                triggerHook(Hook.PROVIDE_TOPIC_PROPERTIES, relTopic.getTopic());
                triggerHook(Hook.PROVIDE_RELATION_PROPERTIES, relTopic.getRelation());
            }
            //
            tx.success();
            return relTopics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Related topics of topic " + topicId + " can't be retrieved", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic")
    @Override
    public List<Topic> searchTopics(@QueryParam("search")    String searchTerm,
                                    @QueryParam("field")     String fieldUri,
                                    @QueryParam("wholeword") boolean wholeWord,
                                    @HeaderParam("Cookie")   ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            List<Topic> searchResult = storage.searchTopics(searchTerm, fieldUri, wholeWord);
            tx.success();
            return searchResult;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Error while searching topics (searchTerm=" + searchTerm + ", fieldUri=" +
                fieldUri + ", wholeWord=" + wholeWord + ", clientContext=" + clientContext + ")", e);
        } finally {
            tx.finish();
        }
    } */

    @POST
    @Path("/topic")
    @Override
    public Topic createTopic(TopicData topicData, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            triggerHook(Hook.PRE_CREATE_TOPIC, topicData, clientContext);
            //
            Topic topic = storage.createTopic(topicData);
            associateWithTopicType(topic);
            //
            Composite comp = topicData.getComposite();
            if (comp != null) {
                storeComposite(topic, comp);
            }
            //
            triggerHook(Hook.POST_CREATE_TOPIC, topic, clientContext);
            triggerHook(Hook.ENRICH_TOPIC, topic, clientContext);
            //
            tx.success();
            return buildTopic(topic, true);
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic failed (" + topicData + ")", e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Path("/topic")
    @Override
    public Topic updateTopic(TopicData topicData, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = getTopic(topicData.getId(), clientContext);
            // Properties oldProperties = new Properties(topic.getProperties());   // copy old properties for comparison
            //
            // ### triggerHook(Hook.PRE_UPDATE_TOPIC, topic, properties);
            //
            // ### storage.setTopicProperties(id, properties);
            Composite comp = topicData.getComposite();
            if (comp != null) {
                storeComposite(topic, comp);
                storage.setTopicValue(topic.getId(), new TopicValue(comp.getLabel()));
            }
            //
            topic = getTopic(topicData.getId(), clientContext);
            // ### topic.setProperties(properties);
            // ### triggerHook(Hook.POST_UPDATE_TOPIC, topic, oldProperties);
            //
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating topic failed (" + topicData + ")", e);
        } finally {
            tx.finish();
        }
    }

    @DELETE
    @Path("/topic/{id}")
    @Override
    public void deleteTopic(@PathParam("id") long topicId, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = getTopic(topicId, clientContext);
            deleteTopic(topic);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Deleting topic " + topicId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    // === Associations ===

    /* @Override
    public Relation getRelation(long id) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Relation relation = storage.getRelation(id);
            tx.success();
            return relation;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Relation " + id + " can't be retrieved", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/relation")
    @Override
    public Relation getRelation(@QueryParam("src") long srcTopicId, @QueryParam("dst") long dstTopicId,
                                @QueryParam("type") String typeId, @QueryParam("directed") boolean isDirected) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Relation relation = storage.getRelation(srcTopicId, dstTopicId, typeId, isDirected);
            tx.success();
            return relation;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Error while retrieving relation between topics " + srcTopicId +
                " and " + dstTopicId + " (typeId=" + typeId + ", isDirected=" + isDirected + ")", e);
        } finally {
            tx.finish();
        }
    } */

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    @Override
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id,
                                            @PathParam("assoc_type_uri") String assocTypeUri) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<Association> assocs = storage.getAssociations(topic1Id, topic2Id, assocTypeUri);
            tx.success();
            return assocs;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving associations between topics " + topic1Id +
                " and " + topic2Id + " failed (assocTypeUri=\"" + assocTypeUri + "\")", e);
        } finally {
            tx.finish();
        }
    }

    @POST
    @Path("/association")
    @Override
    public Association createAssociation(AssociationData assocData,
                                         @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Association assoc = buildAssociation(storage.createAssociation(assocData));
            associateWithAssociationType(assoc);
            //
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating association failed (" + assocData + ")", e);
        } finally {
            tx.finish();
        }
    }

    /* @PUT
    @Path("/relation/{id}")
    @Override
    public void setRelationProperties(@PathParam("id") long id, Properties properties) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.setRelationProperties(id, properties);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Properties of relation " + id + " can't be set (" + properties + ")", e);
        } finally {
            tx.finish();
        }
    } */

    @DELETE
    @Path("/association/{id}")
    @Override
    public void deleteAssociation(@PathParam("id") long assocId, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            triggerHook(Hook.PRE_DELETE_RELATION, assocId);
            storage.deleteAssociation(assocId);
            triggerHook(Hook.POST_DELETE_RELATION, assocId);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Deleting association " + assocId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    // === Types ===

    @GET
    @Path("/topictype")
    @Override
    public Set<String> getTopicTypeUris() {
        Topic metaType = buildTopic(storage.getTopic("uri", new TopicValue("dm3.core.topic_type")), false);
        Set<Topic> topicTypes = metaType.getRelatedTopics("dm3.core.instantiation", "dm3.core.type",
                                                                                    "dm3.core.instance", false);
        Set<String> topicTypeUris = new HashSet();
        for (Topic topicType : topicTypes) {
            topicTypeUris.add(topicType.getUri());
        }
        return topicTypeUris;
    }

    @GET
    @Path("/topictype/{uri}")
    @Override
    public TopicType getTopicType(@PathParam("uri") String uri, @HeaderParam("Cookie") ClientContext clientContext) {
        if (uri == null) {
            throw new IllegalArgumentException("Tried to get a topic type with null URI");
        }
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopicType topicType = typeCache.get(uri);
            triggerHook(Hook.ENRICH_TOPIC_TYPE, topicType, clientContext);
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic type \"" + uri + "\" failed", e);
        } finally {
            tx.finish();
        }
    }

    @POST
    @Path("/topictype")
    @Consumes("application/x-www-form-urlencoded")
    @Override
    public TopicType createTopicType(TopicTypeData topicTypeData, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.createTopic(topicTypeData);
            associateWithTopicType(topic);
            //
            String typeUri = topicTypeData.getUri();
            associateDataType(typeUri, topicTypeData.getDataTypeUri());
            associateTopicTypes(typeUri, topicTypeData.getAssocDefs());
            associateViewConfig(typeUri, topicTypeData.getViewConfig());
            //
            TopicType topicType = typeCache.get(typeUri);
            //
            // Note: the modification must be applied *before* the enrichment.
            // Consider the Access Control plugin: the creator must be set *before* the permissions can be determined.
            triggerHook(Hook.MODIFY_TOPIC_TYPE, topicType, clientContext);
            triggerHook(Hook.ENRICH_TOPIC_TYPE, topicType, clientContext);
            //
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic type \"" + topicTypeData.getUri() +
                "\" failed (" + topicTypeData + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Topic createAssociationType(AssociationTypeData assocTypeData,
                                       @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
           Topic topic = storage.createTopic(assocTypeData);
           associateWithTopicType(topic);
           //
           tx.success();
           return topic;
        } catch (Exception e) {
           logger.warning("ROLLBACK!");
           throw new RuntimeException("Creating association type \"" + assocTypeData.getUri() +
               "\" failed (" + assocTypeData + ")", e);
        } finally {
           tx.finish();
        }
    }

    /* @POST
    @Path("/topictype/{typeUri}")
    @Override
    public void addDataField(@PathParam("typeUri") String typeUri, DataField dataField) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.addDataField(typeUri, dataField);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Data field \"" + dataField.getUri() + "\" can't be added to topic type \"" +
                typeUri + "\"", e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Path("/topictype/{typeUri}")
    @Override
    public void updateDataField(@PathParam("typeUri") String typeUri, DataField dataField) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.updateDataField(typeUri, dataField);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Data field \"" + dataField.getUri() + "\" of topic type \"" +
                typeUri + "\" can't be updated", e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Path("/topictype/{typeUri}/field_order")
    @Override
    public void setDataFieldOrder(@PathParam("typeUri") String typeUri, List<String> fieldUris) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.setDataFieldOrder(typeUri, fieldUris);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Data field order of topic type \"" + typeUri + "\" can't be set", e);
        } finally {
            tx.finish();
        }
    }

    @DELETE
    @Path("/topictype/{typeUri}/field/{fieldUri}")
    @Override
    public void removeDataField(@PathParam("typeUri") String typeUri, @PathParam("fieldUri") String fieldUri) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.removeDataField(typeUri, fieldUri);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Data field \"" + fieldUri + "\" of topic type \"" +
                typeUri + "\" can't be removed", e);
        } finally {
            tx.finish();
        }
    } */

    // === Commands ===

    @POST
    @Path("/command/{command}")
    @Consumes("application/json, multipart/form-data")
    @Override
    public CommandResult executeCommand(@PathParam("command") String command, CommandParams params,
                                        @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Iterator i = triggerHook(Hook.EXECUTE_COMMAND, command, params, clientContext).values().iterator();
            if (!i.hasNext()) {
                throw new RuntimeException("Command is not handled by any plugin");
            }
            CommandResult result = (CommandResult) i.next();
            if (i.hasNext()) {
                throw new RuntimeException("Ambiguity: more than one plugin returned a result");
            }
            tx.success();
            return result;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Executing command \"" + command + "\" failed (params=" + params + ")", e);
        } finally {
            tx.finish();
        }
    }

    // === Plugins ===

    @Override
    public void registerPlugin(Plugin plugin) {
        pluginCache.put(plugin);
    }

    @Override
    public void unregisterPlugin(String pluginId) {
        pluginCache.remove(pluginId);
    }

    @Override
    public Plugin getPlugin(String pluginId) {
        return pluginCache.get(pluginId);
    }

    @GET
    @Path("/plugin")
    @Override
    public Set<PluginInfo> getPluginInfo() {
        final Set info = new HashSet();
        new PluginCache.Iterator() {
            @Override
            void body(Plugin plugin) {
                String pluginFile = plugin.getConfigProperty("clientSidePluginFile");
                info.add(new PluginInfo(plugin.getId(), pluginFile));
            }
        };
        return info;
    }

    @Override
    public void runPluginMigration(Plugin plugin, int migrationNr, boolean isCleanInstall) {
        runMigration(migrationNr, plugin, isCleanInstall);
        plugin.setMigrationNr(migrationNr);
    }

    // === Misc ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return storage.beginTx();
    }

    @Override
    public void pluginsReady() {
        triggerHook(Hook.ALL_PLUGINS_READY);
    }

    @Override
    public void setupDB() {
        DeepaMehtaTransaction tx = beginTx();
        try {
            logger.info("----- Initializing DeepaMehta 3 Core -----");
            boolean isCleanInstall = initDB();
            if (isCleanInstall) {
                setupBootstrapContent();
            }
            runCoreMigrations(isCleanInstall);
            tx.success();
            tx.finish();
            logger.info("----- Initialization of DeepaMehta 3 Core complete -----");
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            tx.finish();
            shutdown();
            throw new RuntimeException("Setting up the database failed", e);
        }
        // Note: we use no finally clause here because in case of error the core service has to be shut down.
    }

    @Override
    public void shutdown() {
        closeDB();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    // === Topic API Delegates ===

    void setTopicValue(long topicId, TopicValue value) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.setTopicValue(topicId, value);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting value for topic " + topicId + " failed (value=" + value + ")", e);
        } finally {
            tx.finish();
        }
    }

    TopicValue getChildTopicValue(Topic parentTopic, String assocDefUri) {
        Topic childTopic = new ChildTopicEvaluator(parentTopic, assocDefUri).getChildTopic();
        if (childTopic != null) {
            return childTopic.getValue();
        }
        return null;
    }

    /**
     * Set a child's topic value. If the child topic does not exist it is created.
     *
     * @param   parentTopic     The parent topic.
     * @param   assocDefUri     The "axis" that leads to the child. The URI of an {@link AssociationDefinition}.
     * @param   value           The value to set. If <code>null</code> nothing is set. The child topic is potentially
     *                          created and returned anyway.
     *
     * @return  The child topic.
     */
    Topic setChildTopicValue(final Topic parentTopic, String assocDefUri, final TopicValue value) {
        try {
            return new ChildTopicEvaluator(parentTopic, assocDefUri) {
                @Override
                void evaluate(Topic childTopic, AssociationDefinition assocDef) {
                    if (childTopic != null) {
                        if (value != null) {
                            childTopic.setValue(value);
                        }
                    } else {
                        // create child topic
                        String topicTypeUri = assocDef.getPartTopicTypeUri();
                        childTopic = createTopic(new TopicData(null, value, topicTypeUri, null), null);
                        setChildTopic(childTopic);
                        // associate child topic
                        AssociationData assocData = new AssociationData(assocDef.getAssocTypeUri());
                        assocData.addTopicRole(new TopicRole(parentTopic.getId(), assocDef.getWholeRoleTypeUri()));
                        assocData.addTopicRole(new TopicRole(childTopic.getId(), assocDef.getPartRoleTypeUri()));
                        createAssociation(assocData, null);     // FIXME: clientContext=null
                    }
                }
            }.getChildTopic();
        } catch (Exception e) {
            throw new RuntimeException("Setting child topic value failed (parentTopic=" + parentTopic +
                ", assocDefUri=\"" + assocDefUri + "\", value=" + value + ")", e);
        }
    }

    @GET
    @Path("/topic/{id}/related_topics")
    public Set<Topic> getRelatedTopics(@PathParam("id") long topicId,
                                       @QueryParam("assoc_type_uri") String assocTypeUri) {
        try {
            Set<Topic> topics = storage.getRelatedTopics(topicId, assocTypeUri);
            //
            /* ### for (RelatedTopic topic : topics) {
                triggerHook(Hook.PROVIDE_TOPIC_PROPERTIES, relTopic.getTopic());
                triggerHook(Hook.PROVIDE_RELATION_PROPERTIES, relTopic.getRelation());
            } */
            //
            return topics;
        } catch (Exception e) {
            throw new RuntimeException("Retrieving related topics of topic " + topicId +
                " failed (assocTypeUri=\"" + assocTypeUri + "\")", e);
        }
    }

    Topic getRelatedTopic(long topicId, String assocTypeUri, String myRoleType, String othersRoleType) {
        Topic topic = storage.getRelatedTopic(topicId, assocTypeUri, myRoleType, othersRoleType);
        return topic != null ? buildTopic(topic, true) : null;
    }

    Set<Topic> getRelatedTopics(long topicId, String assocTypeUri, String myRoleType, String othersRoleType,
                                                                                      boolean includeComposite) {
        Set<Topic> topics = new HashSet();
        for (Topic topic : storage.getRelatedTopics(topicId, assocTypeUri, myRoleType, othersRoleType)) {
            topics.add(buildTopic(topic, includeComposite));
        }
        return topics;
    }

    Set<Association> getAssociations(long topicId, String myRoleType) {
        Set<Association> assocs = new HashSet();
        for (Association assoc : storage.getAssociations(topicId, myRoleType)) {
            assocs.add(buildAssociation(assoc));
        }
        return assocs;
    }

    // === Association API Delegates ===

    void addTopicToAssociation(long assocId, TopicRole topicRole) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.addTopicToAssociation(assocId, topicRole);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Adding topic to association " + assocId + " failed (" + topicRole + ")", e);
        } finally {
            tx.finish();
        }
    }

    void addAssociationToAssociation(long assocId, AssociationRole assocRole) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.addAssociationToAssociation(assocId, assocRole);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Adding association to association " + assocId + " failed (" + assocRole +
                ")", e);
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Attaches this service to a topic retrieved from storage layer.
     *
     * @return  Instance of {@link AttachedTopic}.
     */
    private Topic buildTopic(Topic topic, boolean includeComposite) {
        if (topic == null) {
            throw new IllegalArgumentException("Tried to build an AttachedTopic from a null Topic");
        }
        // set composite
        if (includeComposite) {
            TopicType topicType = getTopicType(topic.getTypeUri(), null);       // FIXME: clientContext=null
            if (topicType.getDataTypeUri().equals("dm3.core.composite")) {
                topic.setComposite(fetchComposite(topic));
            }
        }
        //
        return new AttachedTopic(topic, this);
    }

    /**
     * Attaches this service to an association retrieved from storage layer.
     *
     * @return  Instance of {@link AttachedAssociation}.
     */
    private Association buildAssociation(Association assoc) {
        if (assoc == null) {
            throw new IllegalArgumentException("Tried to build an AttachedAssociation from a null Association");
        }
        //
        return new AttachedAssociation(assoc, this);
    }

    // ---

    private void storeComposite(Topic topic, Composite comp) {
        Iterator<String> i = comp.keys();
        while (i.hasNext()) {
            String assocDefUri = i.next();
            Object value = comp.get(assocDefUri);
            if (value instanceof Composite) {
                Topic childTopic = setChildTopicValue(topic, assocDefUri, null);
                storeComposite(childTopic, (Composite) value);
            } else {
                setChildTopicValue(topic, assocDefUri, new TopicValue(value));
            }
        }
    }

    private Composite fetchComposite(Topic topic) {
        Composite comp = new Composite();
        TopicType topicType = getTopicType(topic.getTypeUri(), null);                       // FIXME: clientContext=null
        for (AssociationDefinition assocDef : topicType.getAssocDefs().values()) {
            String assocDefUri = assocDef.getUri();
            TopicType partTopicType = getTopicType(assocDef.getPartTopicTypeUri(), null);   // FIXME: clientContext=null
            if (partTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                Topic childTopic = new ChildTopicEvaluator(topic, assocDefUri).getChildTopic();
                if (childTopic != null) {
                    comp.put(assocDefUri, fetchComposite(childTopic));
                }
            } else {
                TopicValue value = getChildTopicValue(topic, assocDefUri);
                if (value != null) {
                    comp.put(assocDefUri, value.value());
                }
            }
        }
        return comp;
    }

    private class ChildTopicEvaluator {

        private Topic childTopic;
        private AssociationDefinition assocDef;

        // ---

        private ChildTopicEvaluator(Topic parentTopic, String assocDefUri) {
            getChildTopic(parentTopic, assocDefUri);
            evaluate(childTopic, assocDef);
        }

        // ---

        /**
         * Note: if the caller uses evaluate() to create a missing child topic
         * he must not forget to call setChildTopic().
         */
        void evaluate(Topic childTopic, AssociationDefinition assocDef) {
        }

        // ---

        Topic getChildTopic() {
            return childTopic;
        }

        void setChildTopic(Topic childTopic) {
            this.childTopic = childTopic;
        }

        // ---

        private void getChildTopic(Topic parentTopic, String assocDefUri) {
            TopicType topicType = getTopicType(parentTopic.getTypeUri(), null);     // FIXME: clientContext=null
            this.assocDef = topicType.getAssocDef(assocDefUri);
            String assocTypeUri = assocDef.getAssocTypeUri();
            String wholeRoleTypeUri = assocDef.getWholeRoleTypeUri();
            String  partRoleTypeUri = assocDef.getPartRoleTypeUri();
            //
            this.childTopic = getRelatedTopic(parentTopic.getId(), assocTypeUri, wholeRoleTypeUri, partRoleTypeUri);
        }
    }

    // === Topic/Association Storage ===

    private void associateWithTopicType(Topic topic) {
        try {
            AssociationData assocData = new AssociationData("dm3.core.instantiation");
            assocData.addTopicRole(new TopicRole(topic.getTypeUri(), "dm3.core.type"));
            assocData.addTopicRole(new TopicRole(topic.getId(), "dm3.core.instance"));
            associateWithAssociationType(storage.createAssociation(assocData));
        } catch (Exception e) {
            throw new RuntimeException("Associating topic with topic type \"" +
                topic.getTypeUri() + "\" failed (" + topic + ")", e);
        }
    }

    private void associateWithAssociationType(Association assoc) {
        try {
            AssociationData assocData = new AssociationData("dm3.core.instantiation");
            assocData.addTopicRole(new TopicRole(assoc.getTypeUri(), "dm3.core.type"));
            assocData.addAssociationRole(new AssociationRole(assoc.getId(), "dm3.core.instance"));
            storage.createAssociation(assocData);
        } catch (Exception e) {
            throw new RuntimeException("Associating association with association type \"" +
                assoc.getTypeUri() + "\" failed (" + assoc + ")", e);
        }
    }

    // ---

    /**
     * Recursively deletes a topic in its entirety, that is the topic itself (the <i>whole</i>) and all sub-topics
     * associated via "dm3.core.composition" (the <i>parts</i>).
     */
    private void deleteTopic(Topic topic) {
        // 1) step down recursively
        TopicType topicType = getTopicType(topic.getTypeUri(), null);       // FIXME: clientContext=null
        if (topicType.getDataTypeUri().equals("dm3.core.composite")) {
            for (AssociationDefinition assocDef : topicType.getAssocDefs().values()) {
                String assocDefUri = assocDef.getUri();
                if (assocDef.getAssocTypeUri().equals("dm3.core.composition")) {
                    Topic childTopic = new ChildTopicEvaluator(topic, assocDefUri).getChildTopic();
                    deleteTopic(childTopic);
                }
            }
        }
        // 2) delete topic
        // delete all the topic's relationships first
        for (Association assoc : storage.getAssociations(topic.getId())) {
            deleteAssociation(assoc.getId(), null);                         // FIXME: clientContext=null
        }
        //
        logger.info("Deleting " + topic);
        storage.deleteTopic(topic.getId());
    }

    // === Topic Type Storage ===

    private void associateDataType(String topicTypeUri, String dataTypeUri) {
        AssociationData assocData = new AssociationData("dm3.core.association");
        assocData.addTopicRole(new TopicRole(topicTypeUri, "dm3.core.topic_type"));
        assocData.addTopicRole(new TopicRole(dataTypeUri,  "dm3.core.data_type"));
        createAssociation(assocData, null);                         // FIXME: clientContext=null
    }

    private void associateTopicTypes(String topicTypeUri, Map<String, AssociationDefinition> assocDefs) {
        Association lastAssocDef = null;
        for (AssociationDefinition assocDef : assocDefs.values()) {
            lastAssocDef = associateTopicType(topicTypeUri, assocDef, lastAssocDef);
        }
    }

    private Association associateTopicType(String topicTypeUri, AssociationDefinition assocDef,
                                                                Association lastAssocDef) {
        try {
            Association assoc = createAssociation(assocDef.toAssociationData(), null);  // FIXME: clientContext=null
            putInSequence(topicTypeUri, assoc, lastAssocDef);
            associateViewConfig(assoc, assocDef.getViewConfig());
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Associating topic type \"" + topicTypeUri +
                "\" with topic type \"" + assocDef.getPartTopicTypeUri() + "\" failed", e);
        }
    }

    private void putInSequence(String topicTypeUri, Association assocDef, Association lastAssocDef) {
        if (lastAssocDef == null) {
            // begin sequence
            AssociationData assocData = new AssociationData("dm3.core.association");
            assocData.addTopicRole(new TopicRole(topicTypeUri, "dm3.core.topic_type"));
            assocData.addAssociationRole(new AssociationRole(assocDef.getId(), "dm3.core.first_assoc_def"));
            createAssociation(assocData, null);                     // FIXME: clientContext=null
        } else {
            // continue sequence
            AssociationData assocData = new AssociationData("dm3.core.sequence");
            assocData.addAssociationRole(new AssociationRole(lastAssocDef.getId(), "dm3.core.predecessor"));
            assocData.addAssociationRole(new AssociationRole(assocDef.getId(),     "dm3.core.successor"));
            createAssociation(assocData, null);                     // FIXME: clientContext=null
        }
    }

    // ---

    private void associateViewConfig(String topicTypeUri, ViewConfiguration viewConfig) {
        for (TopicData topicData : viewConfig.getTopicData()) {
            Topic topic = createTopic(topicData, null);             // FIXME: clientContext=null
            AssociationData assocData = new AssociationData("dm3.core.association");
            assocData.addTopicRole(new TopicRole(topicTypeUri,  "dm3.core.topic_type"));
            assocData.addTopicRole(new TopicRole(topic.getId(), "dm3.core.view_config"));
            createAssociation(assocData, null);                     // FIXME: clientContext=null
        }
    }

    private void associateViewConfig(Association assocDef, ViewConfiguration viewConfig) {
        for (TopicData topicData : viewConfig.getTopicData()) {
            Topic topic = createTopic(topicData, null);             // FIXME: clientContext=null
            assocDef.addTopicRole(new TopicRole(topic.getId(), "dm3.core.view_config"));
        }
    }

    // === Plugins ===

    /**
     * Triggers a hook for all installed plugins.
     */
    private Map<String, Object> triggerHook(final Hook hook, final Object... params) {
        final Map resultMap = new HashMap();
        new PluginCache.Iterator() {
            @Override
            void body(Plugin plugin) {
                try {
                    Object result = triggerHook(plugin, hook, params);
                    if (result != null) {
                        resultMap.put(plugin.getId(), result);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Triggering hook " + hook + " of " + plugin + " failed", e);
                }
            }
        };
        return resultMap;
    }

    /**
     * @throws  NoSuchMethodException
     * @throws  IllegalAccessException
     * @throws  InvocationTargetException
     */
    private Object triggerHook(Plugin plugin, Hook hook, Object... params) throws Exception {
        Method hookMethod = plugin.getClass().getMethod(hook.methodName, hook.paramClasses);
        return hookMethod.invoke(plugin, params);
    }

    // === DB ===

    /**
     * @return  <code>true</code> if this is a clean install, <code>false</code> otherwise.
     */
    private boolean initDB() {
        return storage.init();
    }

    private void closeDB() {
        storage.shutdown();
    }

    // ---

    private void setupBootstrapContent() {
        // Before topic types and asscociation types can be created the meta types must created
        // Note: storage low-level call used here ### explain
        Topic topicType = storage.createTopic(new MetaTypeData("dm3.core.topic_type", "Topic Type"));
        storage.createTopic(new MetaTypeData("dm3.core.assoc_type", "Association Type"));
        // Create topic type "Data Type"
        // ### Note: the topic type "Data Type" depends on the data type "Text" and the data type "Text" in turn
        // depends on the topic type "Data Type". To resolve this circle we use a low-level storage call here
        // and postpone the data type association.
        Topic dataType = storage.createTopic(new TopicTypeData("dm3.core.data_type", "Data Type", "dm3.core.text"));
        // Create data type "Text"
        // Note: storage low-level call used here ### explain
        Topic text = storage.createTopic(new TopicData("dm3.core.text", new TopicValue("Text"), "dm3.core.data_type"));
        //
        storage.createTopic(new TopicData("dm3.core.type", new TopicValue("Type"), "dm3.core.role_type"));
        storage.createTopic(new TopicData("dm3.core.instance", new TopicValue("Instance"), "dm3.core.role_type"));
        // Before data type topics can be associated we must create the association type "Association"
        storage.createTopic(new AssociationTypeData("dm3.core.association", "Association"));
        //
        storage.createTopic(new AssociationTypeData("dm3.core.instantiation", "Instantiation"));
        // Postponed data type association
        associateDataType("dm3.core.topic_type", "dm3.core.text");
        associateDataType("dm3.core.assoc_type", "dm3.core.text");
        associateDataType("dm3.core.data_type",  "dm3.core.text");
        //
        associateWithTopicType(topicType);
        associateWithTopicType(dataType);
        associateWithTopicType(text);
    }

    // === Migrations ===

    private void runCoreMigrations(boolean isCleanInstall) {
        int migrationNr = storage.getMigrationNr();
        int requiredMigrationNr = REQUIRED_CORE_MIGRATION;
        int migrationsToRun = requiredMigrationNr - migrationNr;
        logger.info("Running " + migrationsToRun + " core migrations (migrationNr=" + migrationNr +
            ", requiredMigrationNr=" + requiredMigrationNr + ")");
        for (int i = migrationNr + 1; i <= requiredMigrationNr; i++) {
            runCoreMigration(i, isCleanInstall);
        }
    }

    private void runCoreMigration(int migrationNr, boolean isCleanInstall) {
        runMigration(migrationNr, null, isCleanInstall);
        storage.setMigrationNr(migrationNr);
    }

    // ---

    /**
     * Runs a core migration or a plugin migration.
     *
     * @param   migrationNr     Number of the migration to run.
     * @param   plugin          The plugin that provides the migration to run.
     *                          <code>null</code> for a core migration.
     * @param   isCleanInstall  <code>true</code> if the migration is run as part of a clean install,
     *                          <code>false</code> if the migration is run as part of an update.
     */
    private void runMigration(int migrationNr, Plugin plugin, boolean isCleanInstall) {
        MigrationInfo mi = null;
        try {
            mi = new MigrationInfo(migrationNr, plugin);
            if (!mi.success) {
                throw mi.exception;
            }
            // error checks
            if (!mi.isDeclarative && !mi.isImperative) {
                throw new RuntimeException("Neither a types file (" + mi.migrationFile +
                    ") nor a migration class (" + mi.migrationClassName + ") is found");
            }
            if (mi.isDeclarative && mi.isImperative) {
                throw new RuntimeException("Ambiguity: a types file (" + mi.migrationFile +
                    ") AND a migration class (" + mi.migrationClassName + ") are found");
            }
            // run migration
            String runInfo = " (runMode=" + mi.runMode + ", isCleanInstall=" + isCleanInstall + ")";
            if (mi.runMode.equals(MigrationRunMode.CLEAN_INSTALL.name()) == isCleanInstall ||
                mi.runMode.equals(MigrationRunMode.ALWAYS.name())) {
                logger.info("Running " + mi.migrationInfo + runInfo);
                if (mi.isDeclarative) {
                    JSONHelper.readMigrationFile(mi.migrationIn, mi.migrationFile, this);
                } else {
                    Migration migration = (Migration) mi.migrationClass.newInstance();
                    logger.info("Running " + mi.migrationType + " migration class " + mi.migrationClassName);
                    migration.setService(this);
                    migration.run();
                }
                logger.info(mi.migrationType + " migration complete");
            } else {
                logger.info("Do NOT run " + mi.migrationInfo + runInfo);
            }
            logger.info("Updating migration number (" + migrationNr + ")");
        } catch (Exception e) {
            throw new RuntimeException("Running " + mi.migrationInfo + " failed", e);
        }
    }

    // ---

    /**
     * Collects the info required to run a migration.
     */
    private class MigrationInfo {

        String migrationType;       // "core", "plugin"
        String migrationInfo;       // for logging
        String runMode;             // "CLEAN_INSTALL", "UPDATE", "ALWAYS"
        //
        boolean isDeclarative;
        boolean isImperative;
        //
        String migrationFile;       // for declarative migration
        InputStream migrationIn;    // for declarative migration
        //
        String migrationClassName;  // for imperative migration
        Class migrationClass;       // for imperative migration
        //
        boolean success;            // error occurred?
        Exception exception;        // the error

        MigrationInfo(int migrationNr, Plugin plugin) {
            try {
                String configFile = migrationConfigFile(migrationNr);
                InputStream configIn;
                migrationFile = migrationFile(migrationNr);
                migrationType = plugin != null ? "plugin" : "core";
                //
                if (migrationType.equals("core")) {
                    migrationInfo = "core migration " + migrationNr;
                    logger.info("Preparing " + migrationInfo + " ...");
                    configIn     = getClass().getResourceAsStream(configFile);
                    migrationIn  = getClass().getResourceAsStream(migrationFile);
                    migrationClassName = coreMigrationClassName(migrationNr);
                    migrationClass = loadClass(migrationClassName);
                } else {
                    migrationInfo = "migration " + migrationNr + " of plugin \"" + plugin.getName() + "\"";
                    logger.info("Preparing " + migrationInfo + " ...");
                    configIn     = plugin.getResourceAsStream(configFile);
                    migrationIn  = plugin.getResourceAsStream(migrationFile);
                    migrationClassName = plugin.getMigrationClassName(migrationNr);
                    if (migrationClassName != null) {
                        migrationClass = plugin.loadClass(migrationClassName);
                    }
                }
                //
                isDeclarative = migrationIn != null;
                isImperative = migrationClass != null;
                //
                readMigrationConfigFile(configIn, configFile);
                //
                success = true;
            } catch (Exception e) {
                exception = e;
            }
        }

        // ---

        private void readMigrationConfigFile(InputStream in, String configFile) {
            try {
                Properties migrationConfig = new Properties();
                if (in != null) {
                    logger.info("Reading migration config file \"" + configFile + "\"");
                    migrationConfig.load(in);
                } else {
                    logger.info("Using default migration configuration (no migration config file found, " +
                        "tried \"" + configFile + "\")");
                }
                //
                runMode = migrationConfig.getProperty("migrationRunMode", MigrationRunMode.ALWAYS.name());
                MigrationRunMode.valueOf(runMode);  // check if value is valid
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Error in config file \"" + configFile + "\": \"" + runMode +
                    "\" is an invalid value for \"migrationRunMode\"");
            } catch (IOException e) {
                throw new RuntimeException("Config file \"" + configFile + "\" can't be read", e);
            }
        }

        // ---

        private String migrationFile(int migrationNr) {
            return "/migrations/migration" + migrationNr + ".json";
        }

        private String migrationConfigFile(int migrationNr) {
            return "/migrations/migration" + migrationNr + ".properties";
        }

        private String coreMigrationClassName(int migrationNr) {
            return CORE_MIGRATIONS_PACKAGE + ".Migration" + migrationNr;
        }

        // --- Generic Utilities ---

        /**
         * Uses the core bundle's class loader to load a class by name.
         *
         * @return  the class, or <code>null</code> if the class is not found.
         */
        private Class loadClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
