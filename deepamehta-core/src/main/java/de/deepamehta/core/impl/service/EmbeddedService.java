package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.CommandParams;
import de.deepamehta.core.service.CommandResult;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.util.JSONHelper;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

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
import java.util.LinkedHashSet;
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
public class EmbeddedService implements DeepaMehtaService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String CORE_MIGRATIONS_PACKAGE = "de.deepamehta.core.migrations";
    private static final int REQUIRED_CORE_MIGRATION = 3;

    // ---------------------------------------------------------------------------------------------- Instance Variables

            DeepaMehtaStorage storage;
            TypeCache typeCache;

    private PluginCache pluginCache;
    private BundleContext bundleContext;

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

         PRE_CREATE_TOPIC("preCreateHook",  TopicModel.class, ClientContext.class),
        POST_CREATE_TOPIC("postCreateHook", Topic.class, ClientContext.class),
        // ### PRE_UPDATE_TOPIC("preUpdateHook",  Topic.class, Properties.class),
        // ### POST_UPDATE_TOPIC("postUpdateHook", Topic.class, Properties.class),

        POST_RETYPE_ASSOCIATION("postRetypeAssociationHook", Association.class, String.class, Directives.class),

         PRE_DELETE_ASSOCIATION("preDeleteAssociationHook",  Association.class, Directives.class),
        POST_DELETE_ASSOCIATION("postDeleteAssociationHook", Association.class, Directives.class),

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

    public EmbeddedService(DeepaMehtaStorage storage, BundleContext bundleContext) {
        this.storage = storage;
        this.bundleContext = bundleContext;
        this.pluginCache = new PluginCache();
        this.typeCache = new TypeCache(this);
        bootstrapTypeCache();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaService Implementation ***
    // ****************************************



    // === Topics ===

    @GET
    @Path("/topic/{id}")
    @Override
    public AttachedTopic getTopic(@PathParam("id") long topicId,
                                  @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite,
                                  @HeaderParam("Cookie") ClientContext clientContext) {
        logger.info("topicId=" + topicId + ", fetchComposite=" + fetchComposite);
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopic topic = attach(storage.getTopic(topicId), fetchComposite);
            triggerHook(Hook.ENRICH_TOPIC, topic, clientContext);
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic " + topicId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic/by_value/{key}/{value}")
    @Override
    public AttachedTopic getTopic(@PathParam("key") String key, @PathParam("value") SimpleValue value,
                                  @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            TopicModel topic = storage.getTopic(key, value);
            AttachedTopic attachedTopic = topic != null ? attach(topic, fetchComposite) : null;
            tx.success();
            return attachedTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic failed (key=\"" + key + "\", value=" + value + ")", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic/by_type/{type_uri}")
    @Override
    public Set<Topic> getTopics(@PathParam("type_uri") String typeUri) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<Topic> topics = JSONHelper.toTopicSet(getTopicType(typeUri, null).getRelatedTopics(
                "dm4.core.instantiation", "dm4.core.type", "dm4.core.instance", null, false, false));
                // othersTopicTypeUri=null, fetchComposite=false
            /*
            for (Topic topic : topics) {
                triggerHook(Hook.PROVIDE_TOPIC_PROPERTIES, topic);
            }
            */
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topics by type failed (typeUri=\"" + typeUri + "\")", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/topic")
    @Override
    public Set<Topic> searchTopics(@QueryParam("search")    String searchTerm,
                                   @QueryParam("field")     String fieldUri,
                                   @QueryParam("wholeword") boolean wholeWord,
                                   @HeaderParam("Cookie")   ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<Topic> topics = attach(storage.searchTopics(searchTerm, fieldUri, wholeWord), false);
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Searching topics failed (searchTerm=\"" + searchTerm + "\", fieldUri=\"" +
                fieldUri + "\", wholeWord=" + wholeWord + ", clientContext=" + clientContext + ")", e);
        } finally {
            tx.finish();
        }
    }

    @POST
    @Path("/topic")
    @Override
    public AttachedTopic createTopic(TopicModel model, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            triggerHook(Hook.PRE_CREATE_TOPIC, model, clientContext);
            //
            AttachedTopic topic = new AttachedTopic(model, this);
            topic.store();
            //
            triggerHook(Hook.POST_CREATE_TOPIC, topic, clientContext);
            triggerHook(Hook.ENRICH_TOPIC, topic, clientContext);
            //
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic failed (" + model + ")", e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Path("/topic")
    @Override
    public Topic updateTopic(TopicModel model, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopic topic = getTopic(model.getId(), true, clientContext);   // fetchComposite=true ### false?
            //
            // Properties oldProperties = new Properties(topic.getProperties());   // copy old properties for comparison
            // ### triggerHook(Hook.PRE_UPDATE_TOPIC, topic, properties);
            //
            topic.update(model);
            // ### FIXME: avoid refetching. Required is updating the topic model for aggregations: replacing
            // $id composite entries with actual values. See AttachedDeepaMehtaObject.storeComposite()
            topic = getTopic(model.getId(), true, clientContext);  // fetchComposite=true
            //
            // ### triggerHook(Hook.POST_UPDATE_TOPIC, topic, oldProperties);
            //
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating topic failed (" + model + ")", e);
        } finally {
            tx.finish();
        }
    }

    @DELETE
    @Path("/topic/{id}")
    @Override
    public Directives deleteTopic(@PathParam("id") long topicId, @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        Topic topic = null;
        try {
            topic = getTopic(topicId, true, clientContext);   // fetchComposite=true ### false?
            //
            Directives directives = new Directives();
            //
            topic.delete(directives);
            //
            tx.success();
            return directives;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Deleting topic " + topicId + " failed (" + topic + ")", e);
        } finally {
            tx.finish();
        }
    }



    // === Associations ===

    @GET
    @Path("/association/{id}")
    @Override
    public AttachedAssociation getAssociation(@PathParam("id") long assocId) {
        logger.info("assocId=" + assocId);
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociation assoc = attach(storage.getAssociation(assocId));
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving association " + assocId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    @Override
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id) {
        return getAssociations(topic1Id, topic2Id, null);
    }

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    @Override
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id,
                                            @PathParam("assoc_type_uri") String assocTypeUri) {
        logger.info("topic1Id=" + topic1Id + ", topic2Id=" + topic2Id + ", assocTypeUri=\"" + assocTypeUri + "\"");
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<Association> assocs = attach(storage.getAssociations(topic1Id, topic2Id, assocTypeUri));
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

    // ---

    @POST
    @Path("/association")
    @Override
    public Association createAssociation(AssociationModel model,
                                         @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociation assoc = new AttachedAssociation(model, this);
            assoc.store();
            //
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating association failed (" + model + ")", e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Path("/association")
    @Override
    public Directives updateAssociation(AssociationModel model,
                                        @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociation assoc = getAssociation(model.getId());
            //
            // Properties oldProperties = new Properties(topic.getProperties());   // copy old properties for comparison
            // ### triggerHook(Hook.PRE_UPDATE_TOPIC, topic, properties);
            //
            AssociationChangeReport report = assoc.update(model);
            //
            Directives directives = new Directives();
            directives.add(Directive.UPDATE_ASSOCIATION, assoc);
            //
            if (report.typeUriChanged) {
                triggerHook(Hook.POST_RETYPE_ASSOCIATION, assoc, report.oldTypeUri, directives);
            }
            //
            tx.success();
            return directives;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating association failed (" + model + ")", e);
        } finally {
            tx.finish();
        }
    }

    @DELETE
    @Path("/association/{id}")
    @Override
    public Directives deleteAssociation(@PathParam("id") long assocId,
                                        @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        Association assoc = null;
        try {
            assoc = getAssociation(assocId);
            //
            Directives directives = new Directives();
            //
            triggerHook(Hook.PRE_DELETE_ASSOCIATION, assoc, directives);
            assoc.delete(directives);
            triggerHook(Hook.POST_DELETE_ASSOCIATION, assoc, directives);
            //
            tx.success();
            return directives;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Deleting association " + assocId + " failed (" + assoc + ")", e);
        } finally {
            tx.finish();
        }
    }



    // === Topic Types ===

    @GET
    @Path("/topictype")
    @Override
    public Set<String> getTopicTypeUris() {
        Topic metaType = attach(storage.getTopic("uri", new SimpleValue("dm4.core.topic_type")), false);
        Set<RelatedTopic> topicTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
            "dm4.core.instance", "dm4.core.topic_type", false, false);
        Set<String> topicTypeUris = new HashSet();
        // add meta types
        topicTypeUris.add("dm4.core.topic_type");
        topicTypeUris.add("dm4.core.assoc_type");
        topicTypeUris.add("dm4.core.meta_type");
        topicTypeUris.add("dm4.core.meta_meta_type");
        // add regular types
        for (Topic topicType : topicTypes) {
            topicTypeUris.add(topicType.getUri());
        }
        return topicTypeUris;
    }

    @GET
    @Path("/topictype/{uri}")
    @Override
    public AttachedTopicType getTopicType(@PathParam("uri") String uri,
                                          @HeaderParam("Cookie") ClientContext clientContext) {
        if (uri == null) {
            throw new IllegalArgumentException("Tried to get a topic type with null URI");
        }
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopicType topicType = typeCache.getTopicType(uri);
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
    @Override
    public TopicType createTopicType(TopicTypeModel topicTypeModel,
                                     @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopicType topicType = new AttachedTopicType(topicTypeModel, this);
            topicType.store();
            typeCache.put(topicType);
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
            throw new RuntimeException("Creating topic type \"" + topicTypeModel.getUri() +
                "\" failed (" + topicTypeModel + ")", e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Path("/topictype")
    @Override
    public TopicType updateTopicType(TopicTypeModel topicTypeModel,
                                     @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            String topicTypeUri = getTopic(topicTypeModel.getId(), false, clientContext).getUri();  // fetchComp..=false
            AttachedTopicType topicType = getTopicType(topicTypeUri, clientContext);
            //
            // Properties oldProperties = new Properties(topic.getProperties());   // copy old properties for comparison
            // ### triggerHook(Hook.PRE_UPDATE_TOPIC, topic, properties);
            //
            topicType.update(topicTypeModel);
            //
            // ### triggerHook(Hook.POST_UPDATE_TOPIC, topic, oldProperties);
            //
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating topic type failed (" + topicTypeModel + ")", e);
        } finally {
            tx.finish();
        }
    }



    // === Association Types ===

    @GET
    @Path("/assoctype")
    @Override
    public Set<String> getAssociationTypeUris() {
        Topic metaType = attach(storage.getTopic("uri", new SimpleValue("dm4.core.assoc_type")), false);
        Set<RelatedTopic> assocTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
            "dm4.core.instance", "dm4.core.assoc_type", false, false);
        Set<String> assocTypeUris = new HashSet();
        for (Topic assocType : assocTypes) {
            assocTypeUris.add(assocType.getUri());
        }
        return assocTypeUris;
    }

    @GET
    @Path("/assoctype/{uri}")
    @Override
    public AttachedAssociationType getAssociationType(@PathParam("uri") String uri,
                                                      @HeaderParam("Cookie") ClientContext clientContext) {
        if (uri == null) {
            throw new IllegalArgumentException("Tried to get an association type with null URI");
        }
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociationType assocType = typeCache.getAssociationType(uri);
            // ### triggerHook(Hook.ENRICH_TOPIC_TYPE, topicType, clientContext);
            tx.success();
            return assocType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving association type \"" + uri + "\" failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public AssociationType createAssociationType(AssociationTypeModel assocTypeModel,
                                                 @HeaderParam("Cookie") ClientContext clientContext) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociationType assocType = new AttachedAssociationType(assocTypeModel, this);
            assocType.store();
            typeCache.put(assocType);
            //
            tx.success();
            return assocType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating association type \"" + assocTypeModel.getUri() +
                "\" failed (" + assocTypeModel + ")", e);
        } finally {
            tx.finish();
        }
    }



    // === Commands ===

    @POST
    @Path("/command/{command}")
    @Consumes("application/json, multipart/form-data")
    @Override
    public CommandResult executeCommand(@PathParam("command") String command, CommandParams params,
                                        @HeaderParam("Cookie") ClientContext clientContext) {
        logger.info("command=\"" + command + "\", params=" + params);
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
    public void checkAllPluginsReady() {
        Bundle[] bundles = bundleContext.getBundles();
        int plugins = 0;
        int registered = 0;
        for (Bundle bundle : bundles) {
            if (isDeepaMehtaPlugin(bundle)) {
                plugins++;
                if (isPluginRegistered(bundle.getSymbolicName())) {
                    registered++;
                }
            }
        }
        logger.info("### bundles total: " + bundles.length +
            ", DM plugins: " + plugins + ", registered: " + registered);
        if (plugins == registered) {
            logger.info("########## All plugins ready ##########");
            triggerHook(Hook.ALL_PLUGINS_READY);
        }
    }

    @Override
    public void setupDB() {
        DeepaMehtaTransaction tx = beginTx();
        try {
            logger.info("----- Initializing DeepaMehta 4 Core -----");
            boolean isCleanInstall = initDB();
            if (isCleanInstall) {
                setupBootstrapContent();
            }
            runCoreMigrations(isCleanInstall);
            tx.success();
            tx.finish();
            logger.info("----- Completing initialization of DeepaMehta 4 Core -----");
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



    // **********************
    // *** Topic REST API ***
    // **********************



    @GET
    @Path("/topic/{id}/related_topics")
    public Set<RelatedTopic> getRelatedTopics(@PathParam("id")                     long topicId,
                                              @QueryParam("assoc_type_uri")        String assocTypeUri,
                                              @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                              @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                              @QueryParam("others_topic_type_uri") String othersTopicTypeUri) {
        logger.info("topicId=" + topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\"");
        try {
            return getTopic(topicId, false, null).getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
                othersTopicTypeUri, false, false);  // fetchComposite=false (3x)
        } catch (Exception e) {
            throw new RuntimeException("Retrieving related topics of topic " + topicId + " failed (assocTypeUri=\"" +
                assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", othersRoleTypeUri=\"" + othersRoleTypeUri +
                "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")", e);
        }
    }



    // ****************************
    // *** Association REST API ***
    // ****************************

    // ### TODO



    // ----------------------------------------------------------------------------------------- Package Private Methods

    // === Helper ===

    Set<TopicModel> getTopicModels(Set<RelatedTopic> topics) {
        Set<TopicModel> models = new HashSet();
        for (Topic topic : topics) {
            models.add(((AttachedTopic) topic).getModel());
        }
        return models;
    }

    /**
     * Convenience method.
     */
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        // FIXME: clientContext=null
        return createAssociation(new AssociationModel(typeUri, roleModel1, roleModel2), null);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Attaches this core service to a topic retrieved from storage layer.
     */
    AttachedTopic attach(TopicModel model, boolean fetchComposite) {
        AttachedTopic topic = new AttachedTopic(model, this);
        fetchComposite(fetchComposite, topic);
        return topic;
    }

    private Set<Topic> attach(Set<TopicModel> models, boolean fetchComposite) {
        Set<Topic> topics = new LinkedHashSet();
        for (TopicModel model : models) {
            topics.add(attach(model, fetchComposite));
        }
        return topics;
    }

    // ---

    AttachedRelatedTopic attach(RelatedTopicModel model, boolean fetchComposite, boolean fetchRelatingComposite) {
        AttachedRelatedTopic relTopic = new AttachedRelatedTopic(model, this);
        fetchComposite(fetchComposite, fetchRelatingComposite, relTopic);
        return relTopic;
    }

    Set<RelatedTopic> attach(Set<RelatedTopicModel> models, boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedTopic> relTopics = new LinkedHashSet();
        for (RelatedTopicModel model : models) {
            relTopics.add(attach(model, fetchComposite, fetchRelatingComposite));
        }
        return relTopics;
    }

    // ===

    /**
     * Attaches this core service to an association retrieved from storage layer.
     */
    private AttachedAssociation attach(AssociationModel model) {
        return new AttachedAssociation(model, this);
    }

    Set<Association> attach(Set<AssociationModel> models) {
        Set<Association> assocs = new LinkedHashSet();
        for (AssociationModel model : models) {
            assocs.add(attach(model));
        }
        return assocs;
    }

    // ---

    AttachedRelatedAssociation attach(RelatedAssociationModel model) {
        return new AttachedRelatedAssociation(model, this);
    }

    Set<RelatedAssociation> attach(Iterable<RelatedAssociationModel> models,
                                   boolean fetchComposite, boolean fetchRelatingComposite) {
        // TODO: fetch composite
        Set<RelatedAssociation> relAssocs = new LinkedHashSet();
        for (RelatedAssociationModel model : models) {
            relAssocs.add(attach(model));
        }
        return relAssocs;
    }

    // ===

    private void fetchComposite(boolean fetchComposite, AttachedTopic topic) {
        if (fetchComposite) {
            if (topic.getTopicType().getDataTypeUri().equals("dm4.core.composite")) {
                topic.loadComposite();
            }
        }
    }

    private void fetchComposite(boolean fetchComposite, boolean fetchRelatingComposite, AttachedRelatedTopic relTopic) {
        if (fetchComposite) {
            if (relTopic.getTopicType().getDataTypeUri().equals("dm4.core.composite")) {
                relTopic.loadComposite();
            }
        }
        if (fetchRelatingComposite) {
            AttachedAssociation assoc = (AttachedAssociation) relTopic.getAssociation();
            if (assoc.getAssociationType().getDataTypeUri().equals("dm4.core.composite")) {
                assoc.loadComposite();
            }
        }
    }

    // ---

    private void fetchComposite(boolean fetchComposite, AttachedAssociation assoc) {
        if (fetchComposite) {
            if (assoc.getAssociationType().getDataTypeUri().equals("dm4.core.composite")) {
                assoc.loadComposite();
            }
        }
    }



    // === Topic/Association Storage ===

    void associateWithTopicType(TopicModel topic) {
        try {
            AssociationModel model = new AssociationModel("dm4.core.instantiation");
            model.setRoleModel1(new TopicRoleModel(topic.getTypeUri(), "dm4.core.type"));
            model.setRoleModel2(new TopicRoleModel(topic.getId(), "dm4.core.instance"));
            storage.createAssociation(model);
            associateWithAssociationType(model);
            // low-level (storage) call used here ### explain
        } catch (Exception e) {
            throw new RuntimeException("Associating topic with topic type \"" +
                topic.getTypeUri() + "\" failed (" + topic + ")", e);
        }
    }

    void associateWithAssociationType(AssociationModel assoc) {
        try {
            AssociationModel model = new AssociationModel("dm4.core.instantiation");
            model.setRoleModel1(new TopicRoleModel(assoc.getTypeUri(), "dm4.core.type"));
            model.setRoleModel2(new AssociationRoleModel(assoc.getId(), "dm4.core.instance"));
            storage.createAssociation(model);  // low-level (storage) call used here ### explain
        } catch (Exception e) {
            throw new RuntimeException("Associating association with association type \"" +
                assoc.getTypeUri() + "\" failed (" + assoc + ")", e);
        }
    }



    // === Type Storage ===

    // FIXME: move to AttachedType
    /**
     * @param   typeUri     a topic type URI or a association type URI
     */
    void associateDataType(String typeUri, String dataTypeUri) {
        try {
            createAssociation("dm4.core.aggregation",
                new TopicRoleModel(typeUri,     "dm4.core.type"),
                new TopicRoleModel(dataTypeUri, "dm4.core.default"));
        } catch (Exception e) {
            throw new RuntimeException("Associating type \"" + typeUri + "\" with data type \"" +
                dataTypeUri + "\" failed", e);
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

    // ---

    private boolean isDeepaMehtaPlugin(Bundle bundle) {
        String packages = (String) bundle.getHeaders().get("Import-Package");
        // Note: packages might be null. Not all bundles import packges.
        return packages != null && packages.contains("de.deepamehta.core.service") &&
            !bundle.getSymbolicName().equals("de.deepamehta.core");
    }

    private boolean isPluginRegistered(String pluginId) {
        return pluginCache.contains(pluginId);
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



    // === Bootstrap ===

    private void setupBootstrapContent() {
        // Before topic types and asscociation types can be created the meta types must be created
        TopicModel t = new TopicModel("dm4.core.topic_type", "dm4.core.meta_type", new SimpleValue("Topic Type"));
        TopicModel a = new TopicModel("dm4.core.assoc_type", "dm4.core.meta_type", new SimpleValue("Association Type"));
        _createTopic(t);
        _createTopic(a);
        // Create topic type "Data Type"
        // ### Note: the topic type "Data Type" depends on the data type "Text" and the data type "Text" in turn
        // depends on the topic type "Data Type". To resolve this circle we use a low-level (storage) call here
        // and postpone the data type association.
        TopicModel dataType = new TopicTypeModel("dm4.core.data_type", "Data Type", "dm4.core.text");
        TopicModel roleType = new TopicTypeModel("dm4.core.role_type", "Role Type", "dm4.core.text");
        _createTopic(dataType);
        _createTopic(roleType);
        // Create data type "Text"
        TopicModel text = new TopicModel("dm4.core.text", "dm4.core.data_type", new SimpleValue("Text"));
        _createTopic(text);
        // Create role types "Default", "Type", and "Instance"
        TopicModel deflt =    new TopicModel("dm4.core.default",  "dm4.core.role_type", new SimpleValue("Default"));
        TopicModel type =     new TopicModel("dm4.core.type",     "dm4.core.role_type", new SimpleValue("Type"));
        TopicModel instance = new TopicModel("dm4.core.instance", "dm4.core.role_type", new SimpleValue("Instance"));
        _createTopic(deflt);
        _createTopic(type);
        _createTopic(instance);
        // Create association type "Aggregation" -- needed to associate topic/association types with data types
        TopicModel aggregation = new AssociationTypeModel("dm4.core.aggregation", "Aggregation", "dm4.core.text");
        _createTopic(aggregation);
        // Create association type "Instantiation" -- needed to associate topics with topic types
        TopicModel instantiation = new AssociationTypeModel("dm4.core.instantiation", "Instantiation", "dm4.core.text");
        _createTopic(instantiation);
        //
        // 1) Postponed topic type association
        //
        // Note: associateWithTopicType() creates the associations by *low-level* (storage) calls.
        // That's why the associations can be created *before* their type (here: "dm4.core.instantiation")
        // is fully constructed (the type's data type is not yet associated => step 2).
        associateWithTopicType(t);
        associateWithTopicType(a);
        associateWithTopicType(dataType);
        associateWithTopicType(roleType);
        associateWithTopicType(text);
        associateWithTopicType(deflt);
        associateWithTopicType(type);
        associateWithTopicType(instance);
        associateWithTopicType(aggregation);
        associateWithTopicType(instantiation);
        //
        // 2) Postponed data type association
        //
        // Note: associateDataType() creates the association by a *high-level* (service) call.
        // This requires the association type (here: dm4.core.aggregation) to be fully constructed already.
        // That's why the topic type associations (step 1) must be performed *before* the data type associations.
        //
        // Note: at time of the first associateDataType() call the required association type (dm4.core.aggregation)
        // is *not* fully constructed yet! (it gets constructed through this very call). This works anyway because
        // the data type assigning association is created *before* the association type is fetched.
        // (see AttachedAssociation.store(): storage.createAssociation() is called before getType()
        // in AttachedDeepaMehtaObject.store().)
        // Important is that associateDataType("dm4.core.aggregation") is the first call here.
        associateDataType("dm4.core.aggregation",   "dm4.core.text");
        associateDataType("dm4.core.instantiation", "dm4.core.text");
        //
        associateDataType("dm4.core.meta_type",  "dm4.core.text");
        associateDataType("dm4.core.topic_type", "dm4.core.text");
        associateDataType("dm4.core.assoc_type", "dm4.core.text");
        associateDataType("dm4.core.data_type",  "dm4.core.text");
        associateDataType("dm4.core.role_type",  "dm4.core.text");
    }

    private void _createTopic(TopicModel model) {
        // Note: low-level (storage) call used here ### explain
        storage.createTopic(model);
        storage.setTopicValue(model.getId(), model.getSimpleValue());
    }

    private void bootstrapTypeCache() {
        typeCache.put(new AttachedTopicType(new TopicTypeModel("dm4.core.meta_meta_type",
            "dm4.core.meta_meta_meta_type", "Meta Meta Type", "dm4.core.text"), this));
    }



    // === Migrations ===

    /**
     * Determines the core migrations to be run and run them.
     */
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
                logger.info("Completing " + mi.migrationInfo);
            } else {
                logger.info("Running " + mi.migrationInfo + " ABORTED" + runInfo);
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
