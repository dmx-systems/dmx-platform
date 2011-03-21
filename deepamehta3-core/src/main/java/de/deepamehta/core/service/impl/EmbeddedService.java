package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.AssociationType;
import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.CommandParams;
import de.deepamehta.core.model.CommandResult;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.MetaType;
import de.deepamehta.core.model.PluginInfo;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.RelatedTopic;
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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.io.InputStream;
import java.io.IOException;

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
    private static final int REQUIRED_CORE_MIGRATION = 2;

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

    /* @GET
    @Path("/topic/{id}")
    @Override
    public Topic getTopic(@PathParam("id") long id, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.getTopic(id);
            triggerHook(Hook.ENRICH_TOPIC, topic, clientContext);
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Topic " + id + " can't be retrieved", e);
        } finally {
            tx.finish();
        }
    } */

    @GET
    @Path("/topic/by_property/{key}/{value}")
    @Override
    public Topic getTopic(@PathParam("key") String key, @PathParam("value") TopicValue value) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.getTopic(key, value);
            tx.success();
            return topic != null ? buildTopic(topic) : null;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic by value failed (\"" + key + "\"=" + value + ")", e);
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
            Composite comp = topicData.getComposite();
            if (comp != null) {
                createTopicTree(topic, comp);
            }
            //
            triggerHook(Hook.POST_CREATE_TOPIC, topic, clientContext);
            triggerHook(Hook.ENRICH_TOPIC, topic, clientContext);
            //
            tx.success();
            return buildTopic(topic);
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic failed (" + topicData + ")", e);
        } finally {
            tx.finish();
        }
    }

    /* @PUT
    @Path("/topic/{id}")
    @Override
    public void setTopicProperties(@PathParam("id") long id, Properties properties) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = getTopic(id, null);   // clientContext=null
            Properties oldProperties = new Properties(topic.getProperties());   // copy old properties for comparison
            //
            triggerHook(Hook.PRE_UPDATE_TOPIC, topic, properties);
            //
            storage.setTopicProperties(id, properties);
            //
            topic.setProperties(properties);
            triggerHook(Hook.POST_UPDATE_TOPIC, topic, oldProperties);
            //
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting properties of topic " + id + " failed\n" + properties, e);
        } finally {
            tx.finish();
        }
    }

    @DELETE
    @Path("/topic/{id}")
    @Override
    public void deleteTopic(@PathParam("id") long id) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            // delete all the topic's relationships
            for (Relation rel : storage.getRelations(id)) {
                deleteRelation(rel.id);
            }
            //
            storage.deleteTopic(id);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Topic " + id + " can't be deleted", e);
        } finally {
            tx.finish();
        }
    } */

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
    }

    @GET
    @Path("/relation/multiple")
    @Override
    public List<Relation> getRelations(@QueryParam("src") long srcTopicId, @QueryParam("dst") long dstTopicId,
                                       @QueryParam("type") String typeId, @QueryParam("directed") boolean isDirected) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            List<Relation> relations = storage.getRelations(srcTopicId, dstTopicId, typeId, isDirected);
            tx.success();
            return relations;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Error while retrieving relations between topics " + srcTopicId +
                " and " + dstTopicId + " (typeId=" + typeId + ", isDirected=" + isDirected + ")", e);
        } finally {
            tx.finish();
        }
    } */

    @POST
    @Path("/relation/{src}/{dst}/{typeId}")
    @Override
    public Association createAssociation(Association assoc, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            assoc = storage.createAssociation(assoc);
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating " + assoc + " failed", e);
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
    }

    @DELETE
    @Path("/relation/{id}")
    @Override
    public void deleteRelation(@PathParam("id") long id) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            triggerHook(Hook.PRE_DELETE_RELATION, id);
            storage.deleteRelation(id);
            triggerHook(Hook.POST_DELETE_RELATION, id);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Relation " + id + " can't be deleted", e);
        } finally {
            tx.finish();
        }
    } */

    // === Types ===

    @GET
    @Path("/topictype")
    @Override
    public Set<String> getTopicTypeUris() {
        Topic metaType = buildTopic(storage.getTopic("uri", new TopicValue("dm3.core.topic_type")));
        Set<Topic> topicTypes = metaType.getRelatedTopics("dm3.core.instantiation", "dm3.core.type",
                                                                                    "dm3.core.instance");
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
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopicType topicType = typeCache.get(uri);
            // enrichment
            EnrichedTopicType enrichedTopicType = enrichTopicType(topicType, clientContext);
            //
            tx.success();
            return enrichedTopicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic type \"" + uri + "\" failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public MetaType createMetaType(MetaType metaType) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            metaType = storage.createMetaType(metaType);
            tx.success();
            return metaType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creation of " + metaType + " failed", e);
        } finally {
            tx.finish();
        }
    }

    @POST
    @Path("/topictype")
    @Consumes("application/x-www-form-urlencoded")
    @Override
    public Topic createTopicType(TopicTypeData topicTypeData, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Topic topic = storage.createTopic(topicTypeData);
            // TODO: process dataTypeUri
            //
            // Note: the modification must be applied *before* the enrichment.
            // Consider the Access Control plugin: the creator must be set *before* the permissions can be determined.
            triggerHook(Hook.MODIFY_TOPIC_TYPE, topicTypeData, clientContext);
            triggerHook(Hook.ENRICH_TOPIC_TYPE, topicTypeData, clientContext);
            //
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic type failed (" + topicTypeData + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public AssociationType createAssociationType(AssociationType assocType, ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            assocType = storage.createAssociationType(assocType);
            tx.success();
            return assocType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creation of " + assocType + " failed", e);
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
            boolean isCleanInstall = initDB();
            runCoreMigrations(isCleanInstall);
            tx.success();
            tx.finish();
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

    TopicValue getTopicValue(Topic topic, String assocDefUri) {
        TopicType topicType = getTopicType(topic.getTypeUri(), null);   // FIXME: clientContext=null
        AssociationDefinition assocDef = topicType.getAssociationDefinition(assocDefUri);
        String assocTypeUri = assocDef.getAssocTypeUri();
        String wholeRoleTypeUri = assocDef.getWholeRoleTypeUri();
        String  partRoleTypeUri = assocDef.getPartRoleTypeUri();
        //
        Topic childTopic = getRelatedTopic(topic.getId(), assocTypeUri, wholeRoleTypeUri, partRoleTypeUri);
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
    Topic setChildTopicValue(Topic parentTopic, String assocDefUri, TopicValue value) {
        TopicType topicType = getTopicType(parentTopic.getTypeUri(), null);     // FIXME: clientContext=null
        AssociationDefinition assocDef = topicType.getAssociationDefinition(assocDefUri);
        String assocTypeUri = assocDef.getAssocTypeUri();
        String wholeRoleTypeUri = assocDef.getWholeRoleTypeUri();
        String  partRoleTypeUri = assocDef.getPartRoleTypeUri();
        //
        Topic childTopic = getRelatedTopic(parentTopic.getId(), assocTypeUri, wholeRoleTypeUri, partRoleTypeUri);
        if (childTopic != null) {
            if (value != null) {
                childTopic.setValue(value);
            }
        } else {
            // create child topic
            String topicTypeUri = assocDef.getPartTopicTypeUri();
            childTopic = createTopic(new TopicData(null, value, topicTypeUri, null), null);
            // create association
            Set<Role> roles = new HashSet();
            roles.add(new Role(parentTopic.getId(), wholeRoleTypeUri));
            roles.add(new Role(childTopic.getId(), partRoleTypeUri));
            createAssociation(new Association(-1, assocTypeUri, roles), null);
        }
        return childTopic;
    }

    Topic getRelatedTopic(long topicId, String assocTypeUri, String myRoleType, String othersRoleType) {
        Topic topic = storage.getRelatedTopic(topicId, assocTypeUri, myRoleType, othersRoleType);
        return topic != null ? buildTopic(topic) : null;
    }

    Set<Topic> getRelatedTopics(long topicId, String assocTypeUri, String myRoleType, String othersRoleType) {
        Set<Topic> topics = storage.getRelatedTopics(topicId, assocTypeUri, myRoleType, othersRoleType);
        return topics;
        // return topic != null ? buildTopic(topic) : null;     FIXME: attach topics
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic buildTopic(Topic topic) {
        if (topic == null) {
            throw new NullPointerException("Tried to build an AttachedTopic from a null Topic");
        }
        return new AttachedTopic(topic, this);
    }

    private void createTopicTree(Topic topic, Composite comp) {
        Iterator<String> i = comp.keys();
        while (i.hasNext()) {
            String key = i.next();
            Object value = comp.get(key);
            if (value instanceof Composite) {
                Topic childTopic = setChildTopicValue(topic, key, null);
                createTopicTree(childTopic, (Composite) value);
            } else {
                setChildTopicValue(topic, key, new TopicValue(value));
            }
        }
    }

    private EnrichedTopicType enrichTopicType(final AttachedTopicType topicType, ClientContext clientContext) {
        final Map result = triggerHook(Hook.ENRICH_TOPIC_TYPE, topicType, clientContext);
        final EnrichedTopicType enrichedTopicType = new EnrichedTopicType(topicType);
        new PluginCache.Iterator() {
            @Override
            void body(Plugin plugin) {
                Map<String, Object> staticTypeConfig = plugin.getTypeConfig(topicType.getUri());
                Map<String, Object> dynamicEnrichment = (Map) result.get(plugin.getId());
                if (staticTypeConfig != null) {
                    enrichedTopicType.addEnrichment(staticTypeConfig);
                }
                if (dynamicEnrichment != null) {
                    enrichedTopicType.addEnrichment(dynamicEnrichment);
                }
            }
        };
        return enrichedTopicType;
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
