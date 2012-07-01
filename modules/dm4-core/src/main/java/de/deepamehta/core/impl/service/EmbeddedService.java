package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
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
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.CommandParams;
import de.deepamehta.core.service.CommandResult;
import de.deepamehta.core.service.CoreEvent;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Hook;     // ### TODO: drop this
import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ObjectFactory;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.util.JSONHelper;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

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
public class EmbeddedService implements DeepaMehtaService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String CORE_MIGRATIONS_PACKAGE = "de.deepamehta.core.migrations";
    private static final int REQUIRED_CORE_MIGRATION = 3;

    // ---------------------------------------------------------------------------------------------- Instance Variables

            DeepaMehtaStorage storage;
            ObjectFactoryImpl objectFactory;
            TypeCache typeCache;

    private PluginCache pluginCache;
    private ListenerRegistry listenerRegistry;
    private BundleContext bundleContext;

    private enum MigrationRunMode {
        CLEAN_INSTALL, UPDATE, ALWAYS
    }

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public EmbeddedService(DeepaMehtaStorage storage, BundleContext bundleContext) {
        this.storage = storage;
        this.bundleContext = bundleContext;
        this.pluginCache = new PluginCache();
        this.listenerRegistry = new ListenerRegistry();
        this.typeCache = new TypeCache(this);
        this.objectFactory = new ObjectFactoryImpl(this);
        bootstrapTypeCache();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaService Implementation ***
    // ****************************************



    // === Topics ===

    @Override
    public AttachedTopic getTopic(long topicId, boolean fetchComposite, ClientState clientState) {
        // logger.info("topicId=" + topicId + ", fetchComposite=" + fetchComposite);
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopic topic = attach(storage.getTopic(topicId), fetchComposite, clientState);
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic " + topicId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public AttachedTopic getTopic(String key, SimpleValue value, boolean fetchComposite, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            TopicModel model = storage.getTopic(key, value);
            AttachedTopic topic = model != null ? attach(model, fetchComposite, clientState) : null;
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public ResultSet<Topic> getTopics(String typeUri, boolean fetchComposite, int maxResultSize,
                                                                              ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            ResultSet<Topic> topics = JSONHelper.toTopicSet(getTopicType(typeUri, clientState).getRelatedTopics(
                "dm4.core.instantiation", "dm4.core.type", "dm4.core.instance", null, fetchComposite, false,
                maxResultSize, clientState));   // othersTopicTypeUri=null
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

    @Override
    public Set<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            // ### FIXME: fetchComposite=false, parameterize it
            Set<Topic> topics = attach(storage.searchTopics(searchTerm, fieldUri, wholeWord), false, clientState);
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Searching topics failed (searchTerm=\"" + searchTerm + "\", fieldUri=\"" +
                fieldUri + "\", wholeWord=" + wholeWord + ", clientState=" + clientState + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public AttachedTopic createTopic(TopicModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            fireEvent(CoreEvent.PRE_CREATE_TOPIC, model, clientState);
            //
            AttachedTopic topic = new AttachedTopic(model, this);
            Directives directives = new Directives();   // ### FIXME: directives are ignored
            topic.store(clientState, directives);
            //
            fireEvent(CoreEvent.POST_CREATE_TOPIC, topic, clientState, directives);
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

    @Override
    public Directives updateTopic(TopicModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopic topic = getTopic(model.getId(), true, clientState);     // fetchComposite=true
            Directives directives = new Directives();
            //
            topic.update(model, clientState, directives);
            //
            tx.success();
            return directives;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating topic failed (" + model + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Directives deleteTopic(long topicId, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        Topic topic = null;
        try {
            topic = getTopic(topicId, true, clientState);   // fetchComposite=true ### false?
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

    @Override
    public Association getAssociation(long assocId, boolean fetchComposite, ClientState clientState) {
        logger.info("assocId=" + assocId + ", fetchComposite=" + fetchComposite + ", clientState=" + clientState);
        DeepaMehtaTransaction tx = beginTx();
        try {
            Association assoc = attach(storage.getAssociation(assocId), fetchComposite);
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving association " + assocId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                      String roleTypeUri2, boolean fetchComposite, ClientState clientState) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\", fetchComposite=" +
            fetchComposite + ", clientState=" + clientState;
        logger.info(info);
        DeepaMehtaTransaction tx = beginTx();
        try {
            AssociationModel model = storage.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1,
                roleTypeUri2);
            Association assoc = model != null ? attach(model, fetchComposite) : null;
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving association failed (" + info + ")", e);
        } finally {
            tx.finish();
        }
    }

    // ---

    @Override
    public Set<Association> getAssociations(long topic1Id, long topic2Id) {
        return getAssociations(topic1Id, topic2Id, null);
    }

    @Override
    public Set<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        logger.info("topic1Id=" + topic1Id + ", topic2Id=" + topic2Id + ", assocTypeUri=\"" + assocTypeUri + "\"");
        DeepaMehtaTransaction tx = beginTx();
        try {
            // ### FIXME: fetchComposite=false, parameterize it
            Set<Association> assocs = attach(storage.getAssociations(topic1Id, topic2Id, assocTypeUri), false);
            tx.success();
            return assocs;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving associations between topics " + topic1Id + " and " + topic2Id +
                " failed (assocTypeUri=\"" + assocTypeUri + "\")", e);
        } finally {
            tx.finish();
        }
    }

    // ---

    @Override
    public Association createAssociation(AssociationModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociation assoc = new AttachedAssociation(model, this);
            Directives directives = new Directives();   // ### FIXME: directives are ignored
            assoc.store(clientState, directives);
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

    @Override
    public Directives updateAssociation(AssociationModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Association assoc = getAssociation(model.getId(), false, null);     // fetchComposite=false
            Directives directives = new Directives();
            //
            assoc.update(model, clientState, directives);
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

    @Override
    public Directives deleteAssociation(long assocId, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        Association assoc = null;
        try {
            assoc = getAssociation(assocId, false, null);   // fetchComposite=false
            //
            Directives directives = new Directives();
            //
            fireEvent(CoreEvent.PRE_DELETE_ASSOCIATION, assoc, directives);
            assoc.delete(directives);
            fireEvent(CoreEvent.POST_DELETE_ASSOCIATION, assoc, directives);
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

    @Override
    public Set<String> getTopicTypeUris() {
        try {
            Topic metaType = attach(storage.getTopic("uri", new SimpleValue("dm4.core.topic_type")), false, null);
            ResultSet<RelatedTopic> topicTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.topic_type", false, false, 0, null);
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
        } catch (Exception e) {
            throw new RuntimeException("Retrieving list of topic type URIs failed", e);
        }
    }

    @Override
    public AttachedTopicType getTopicType(String uri, ClientState clientState) {
        if (uri == null) {
            throw new IllegalArgumentException("Tried to get a topic type with null URI");
        }
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopicType topicType = typeCache.getTopicType(uri);
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Retrieving topic type \"" + uri + "\" failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public TopicType createTopicType(TopicTypeModel topicTypeModel, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopicType topicType = new AttachedTopicType(topicTypeModel, this);
            // Note: the topic type is put in type cache *before* it is stored. See createAssociationType().
            typeCache.put(topicType);
            topicType.store();
            //
            // Note: the modification must be applied *before* the enrichment. ### FIXDOC
            // Consider the Access Control plugin: the creator must be set *before* the permissions can be determined.
            fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType, clientState);
            //
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic type \"" + topicTypeModel.getUri() + "\" failed (" +
                topicTypeModel + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Directives updateTopicType(TopicTypeModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            String topicTypeUri = getTopic(model.getId(), false, clientState).getUri();     // fetchComposite=false
            AttachedTopicType topicType = getTopicType(topicTypeUri, clientState);
            Directives directives = new Directives();
            //
            topicType.update(model, clientState, directives);
            //
            tx.success();
            return directives;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating topic type failed (" + model + ")", e);
        } finally {
            tx.finish();
        }
    }



    // === Association Types ===

    @Override
    public Set<String> getAssociationTypeUris() {
        try {
            Topic metaType = attach(storage.getTopic("uri", new SimpleValue("dm4.core.assoc_type")), false, null);
            ResultSet<RelatedTopic> assocTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.assoc_type", false, false, 0, null);
            Set<String> assocTypeUris = new HashSet();
            for (Topic assocType : assocTypes) {
                assocTypeUris.add(assocType.getUri());
            }
            return assocTypeUris;
        } catch (Exception e) {
            throw new RuntimeException("Retrieving list of association type URIs failed", e);
        }
    }

    @Override
    public AttachedAssociationType getAssociationType(String uri, ClientState clientState) {
        if (uri == null) {
            throw new IllegalArgumentException("Tried to get an association type with null URI");
        }
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociationType assocType = typeCache.getAssociationType(uri);
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
    public AssociationType createAssociationType(AssociationTypeModel assocTypeModel, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedAssociationType assocType = new AttachedAssociationType(assocTypeModel, this);
            // Note: the association type must be put in type cache *before* it is stored.
            // Storing an object requires its data type to be known. See AttachedDeepaMehtaObject.store()
            // Consider creation of association type "Composition Definition": it has a composition definition itself.
            typeCache.put(assocType);
            assocType.store();
            //
            tx.success();
            return assocType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating association type \"" + assocTypeModel.getUri() + "\" failed (" +
                assocTypeModel + ")", e);
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
    public void unregisterPlugin(String pluginUri) {
        pluginCache.remove(pluginUri);
    }

    @Override
    public Plugin getPlugin(String pluginUri) {
        return pluginCache.get(pluginUri);
    }

    @Override
    public Set<PluginInfo> getPluginInfo() {
        final Set info = new HashSet();
        new PluginCache.Iterator() {
            @Override
            void body(Plugin plugin) {
                info.add(plugin.getInfo());
            }
        };
        return info;
    }

    @Override
    public void runPluginMigration(Plugin plugin, int migrationNr, boolean isCleanInstall) {
        runMigration(migrationNr, plugin, isCleanInstall);
        plugin.setMigrationNr(migrationNr);
    }

    /**
     * Triggers a hook for all installed plugins.
     *
     * ### TODO: drop this method.
     */
    @Override
    public Map<String, Object> triggerHook(final Hook hook, final Object... params) {
        final Map resultMap = new HashMap();
        new PluginCache.Iterator() {
            @Override
            void body(Plugin plugin) {
                try {
                    Object result = triggerHook(plugin, hook, params);
                    if (result != null) {
                        resultMap.put(plugin.getUri(), result);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Triggering hook " + hook + " of " + plugin + " failed", e);
                }
            }
        };
        return resultMap;
    }



    // === Listeners ===

    @Override
    public void addListener(CoreEvent event, Listener listener) {
        listenerRegistry.addListener(event, listener);
    }

    @Override
    public void removeListener(CoreEvent event, Listener listener) {
        listenerRegistry.removeListener(event, listener);
    }

    // ---

    @Override
    public List<Object> fireEvent(CoreEvent event, Object... params) {
        return listenerRegistry.fireEvent(event, params);
    }

    @Override
    public Object deliverEvent(Listener listener, CoreEvent event, Object... params) {
        return listenerRegistry.deliverEvent(listener, event, params);
    }



    // === Misc ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return storage.beginTx();
    }

    @Override
    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }


    @Override
    public void checkAllPluginsReady() {
        Bundle[] bundles = bundleContext.getBundles();
        int plugins = 0;
        int ready = 0;
        for (Bundle bundle : bundles) {
            if (isDeepaMehtaPlugin(bundle)) {
                plugins++;
                if (isPluginRegistered(bundle.getSymbolicName())) {
                    ready++;
                }
            }
        }
        logger.info("### Bundles total: " + bundles.length +
            ", DeepaMehta plugins: " + plugins + ", Ready: " + ready);
        if (plugins == ready) {
            logger.info("########## All plugins ready ##########");
            fireEvent(CoreEvent.ALL_PLUGINS_READY);
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
            logger.info("----- Initialization of DeepaMehta 4 Core complete -----");
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
        // FIXME: clientState=null
        return createAssociation(new AssociationModel(typeUri, roleModel1, roleModel2), null);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Attaches this core service to a topic model fetched from storage layer.
     * Optionally fetches the topic's composite value from storage layer.
     */
    AttachedTopic attach(TopicModel model, boolean fetchComposite, ClientState clientState) {
        AttachedTopic topic = new AttachedTopic(model, this);
        fetchComposite(topic, fetchComposite);
        return topic;
    }

    private Set<Topic> attach(Set<TopicModel> models, boolean fetchComposite, ClientState clientState) {
        Set<Topic> topics = new LinkedHashSet();
        for (TopicModel model : models) {
            topics.add(attach(model, fetchComposite, clientState));
        }
        return topics;
    }

    // ---

    AttachedRelatedTopic attach(RelatedTopicModel model, boolean fetchComposite, boolean fetchRelatingComposite,
                                                                                 ClientState clientState) {
        AttachedRelatedTopic relTopic = new AttachedRelatedTopic(model, this);
        fetchComposite(relTopic, fetchComposite, fetchRelatingComposite);
        return relTopic;
    }

    ResultSet<RelatedTopic> attach(ResultSet<RelatedTopicModel> models, boolean fetchComposite,
                                   boolean fetchRelatingComposite, ClientState clientState) {
        Set<RelatedTopic> relTopics = new LinkedHashSet();
        for (RelatedTopicModel model : models) {
            relTopics.add(attach(model, fetchComposite, fetchRelatingComposite, clientState));
        }
        return new ResultSet<RelatedTopic>(models.getTotalCount(), relTopics);
    }

    // ===

    /**
     * Attaches this core service to an association fetched from storage layer.
     * Optionally fetches the topic's composite value from storage layer.
     */
    private AttachedAssociation attach(AssociationModel model, boolean fetchComposite) {
        AttachedAssociation assoc = new AttachedAssociation(model, this);
        fetchComposite(assoc, fetchComposite);
        return assoc;
    }

    Set<Association> attach(Set<AssociationModel> models, boolean fetchComposite) {
        Set<Association> assocs = new LinkedHashSet();
        for (AssociationModel model : models) {
            assocs.add(attach(model, fetchComposite));
        }
        return assocs;
    }

    // ---

    AttachedRelatedAssociation attach(RelatedAssociationModel model) {
        return new AttachedRelatedAssociation(model, this);
        // ### TODO: fetch composite
    }

    Set<RelatedAssociation> attach(Iterable<RelatedAssociationModel> models,
                                   boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedAssociation> relAssocs = new LinkedHashSet();
        for (RelatedAssociationModel model : models) {
            relAssocs.add(attach(model));
        }
        return relAssocs;
    }

    // ===

    private void fetchComposite(AttachedTopic topic, boolean fetchComposite) {
        if (fetchComposite) {
            if (topic.getTopicType().getDataTypeUri().equals("dm4.core.composite")) {
                topic.loadComposite();
            }
        }
    }

    private void fetchComposite(AttachedRelatedTopic relTopic, boolean fetchComposite, boolean fetchRelatingComposite) {
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

    private void fetchComposite(AttachedAssociation assoc, boolean fetchComposite) {
        if (fetchComposite) {
            if (assoc.getAssociationType().getDataTypeUri().equals("dm4.core.composite")) {
                assoc.loadComposite();
            }
        }
    }



    // === Topic/Association Storage ===

    void associateWithTopicType(TopicModel topic) {
        try {
            // check argument
            if (topic.getTypeUri() == null) {
                throw new IllegalArgumentException("The type of topic " + topic.getId() + " is unknown (null)");
            }
            //
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
     * ### TODO: drop this method
     *
     * @throws  NoSuchMethodException
     * @throws  IllegalAccessException
     * @throws  InvocationTargetException
     */
    private Object triggerHook(Plugin plugin, Hook hook, Object... params) throws Exception {
        Method hookMethod = plugin.getClass().getMethod(hook.getMethodName(), hook.getParamClasses());
        return hookMethod.invoke(plugin, params);
    }

    private boolean isDeepaMehtaPlugin(Bundle bundle) {
        String packages = (String) bundle.getHeaders().get("Import-Package");
        // Note: packages might be null. Not all bundles import packges.
        return packages != null && packages.contains("de.deepamehta.core.service") &&
            !bundle.getSymbolicName().equals("de.deepamehta.core");
    }

    private boolean isPluginRegistered(String pluginUri) {
        return pluginCache.contains(pluginUri);
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
                String message = "Neither a migration file (" + mi.migrationFile + ") nor a migration class ";
                if (mi.migrationClassName != null) {
                    throw new RuntimeException(message + "(" + mi.migrationClassName + ") is found");
                } else {
                    throw new RuntimeException(message + "is found. Note: a possible migration class can't be located" +
                        " (plugin package is unknown). Consider setting \"pluginPackage\" in plugin.properties");
                }
            }
            if (mi.isDeclarative && mi.isImperative) {
                throw new RuntimeException("Ambiguity: a migration file (" + mi.migrationFile + ") AND a migration " +
                    "class (" + mi.migrationClassName + ") are found. Consider using two different migration numbers.");
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
                    logger.info("Reading migration config file \"" + configFile + "\" ABORTED -- file does not exist");
                }
                //
                runMode = migrationConfig.getProperty("migrationRunMode", MigrationRunMode.ALWAYS.name());
                MigrationRunMode.valueOf(runMode);  // check if value is valid
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Reading migration config file \"" + configFile + "\" failed: \"" + runMode +
                    "\" is an invalid value for \"migrationRunMode\"", e);
            } catch (IOException e) {
                throw new RuntimeException("Reading migration config file \"" + configFile + "\" failed", e);
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
