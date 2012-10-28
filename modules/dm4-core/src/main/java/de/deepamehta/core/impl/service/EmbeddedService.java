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
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ObjectFactory;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.accesscontrol.AccessControlList;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.osgi.framework.BundleContext;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Implementation of the DeepaMehta core service. Embeddable into Java applications.
 */
public class EmbeddedService implements DeepaMehtaService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    PluginManager pluginManager;
    ListenerRegistry listenerRegistry;
    DeepaMehtaStorage storage;
    MigrationManager migrationManager;
    ObjectFactoryImpl objectFactory;
    TypeCache typeCache;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DeepaMehta 4 Core bundle.
     */
    public EmbeddedService(DeepaMehtaStorage storage, BundleContext bundleContext) {
        this.storage = storage;
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(bundleContext);
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
        // logger.info("topicId=" + topicId + ", fetchComposite=" + fetchComposite + ", clientState=" + clientState);
        DeepaMehtaTransaction tx = beginTx();
        try {
            AttachedTopic topic = attach(storage.getTopic(topicId), fetchComposite, clientState);
            tx.success();
            return topic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
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
            throw new RuntimeException("Fetching topic failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public ResultSet<Topic> getTopics(String typeUri, boolean fetchComposite, int maxResultSize,
                                                                              ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            ResultSet<Topic> topics = DeepaMehtaUtils.toTopicSet(getTopicType(typeUri, clientState).getRelatedTopics(
                "dm4.core.instantiation", "dm4.core.type", "dm4.core.instance", null, fetchComposite, false,
                maxResultSize, clientState));   // othersTopicTypeUri=null
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching topics by type failed (typeUri=\"" + typeUri + "\")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Set<Topic> searchTopics(String searchTerm, String fieldUri, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            // ### FIXME: fetchComposite=false, parameterize it
            Set<Topic> topics = attach(storage.searchTopics(searchTerm, fieldUri), false, clientState);
            tx.success();
            return topics;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Searching topics failed (searchTerm=\"" + searchTerm + "\", fieldUri=\"" +
                fieldUri + "\", clientState=" + clientState + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Topic createTopic(TopicModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            fireEvent(CoreEvent.PRE_CREATE_TOPIC, model, clientState);
            //
            Directives directives = new Directives();   // ### FIXME: directives are ignored
            Topic topic = objectFactory.storeTopic(model, clientState, directives);
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
            AttachedTopic topic = getTopic(model.getId(), true, clientState);   // fetchComposite=true
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
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
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
        // logger.info(info);   ### TODO: the Access Control plugin calls getAssociation() very often. It should cache.
        DeepaMehtaTransaction tx = beginTx();
        try {
            AssociationModel model = storage.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1,
                roleTypeUri2);
            Association assoc = model != null ? attach(model, fetchComposite) : null;
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Association getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                String topicRoleTypeUri, String assocRoleTypeUri,
                                                                boolean fetchComposite, ClientState clientState) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topicId=" + topicId + ", assocId=" + assocId +
            ", topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri +
            "\", fetchComposite=" + fetchComposite + ", clientState=" + clientState;
        logger.info(info);
        DeepaMehtaTransaction tx = beginTx();
        try {
            AssociationModel model = storage.getAssociationBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
                topicRoleTypeUri, assocRoleTypeUri);
            Association assoc = model != null ? attach(model, fetchComposite) : null;
            tx.success();
            return assoc;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        } finally {
            tx.finish();
        }
    }

    // ---

    @Override
    public Set<RelatedAssociation> getAssociations(String assocTypeUri) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<RelatedAssociation> assocs = getAssociationType(assocTypeUri, null).getRelatedAssociations(
                null, "dm4.core.type", "dm4.core.instance", null, false, false);
                // ### FIXME: assocTypeUri=null but should be "dm4.core.instantiation", but not stored for assocs.
                // othersAssocTypeUri=null, fetchComposite=false, fetchRelatingComposite=false
            tx.success();
            return assocs;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching associations by type failed (assocTypeUri=\"" + assocTypeUri + "\")",
                e);
        } finally {
            tx.finish();
        }
    }

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
            throw new RuntimeException("Fetching associations between topics " + topic1Id + " and " + topic2Id +
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
            fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model, clientState);
            //
            Directives directives = new Directives();   // ### FIXME: directives are ignored
            Association assoc = objectFactory.storeAssociation(model, clientState, directives);
            //
            fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, assoc, clientState, directives);
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
            Association assoc = getAssociation(model.getId(), true, null);      // fetchComposite=true
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
            throw new RuntimeException("Fetching list of topic type URIs failed", e);
        }
    }

    @Override
    public TopicType getTopicType(String uri, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            TopicType topicType = typeCache.getTopicType(uri);
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching topic type \"" + uri + "\" failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Set<TopicType> getAllTopicTypes(ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<TopicType> topicTypes = new HashSet();
            for (String uri : getTopicTypeUris()) {
                TopicType topicType = getTopicType(uri, clientState);
                topicTypes.add(topicType);
            }
            tx.success();
            return topicTypes;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching all topic types failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public TopicType createTopicType(TopicTypeModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            objectFactory.storeType(model);
            AttachedTopicType topicType = new AttachedTopicType(model, this);
            typeCache.put(topicType);
            //
            fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType, clientState);
            //
            tx.success();
            return topicType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating topic type \"" + model.getUri() + "\" failed (" + model + ")", e);
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
            TopicType topicType = getTopicType(topicTypeUri, clientState);
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
            throw new RuntimeException("Fetching list of association type URIs failed", e);
        }
    }

    @Override
    public AssociationType getAssociationType(String uri, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AssociationType assocType = typeCache.getAssociationType(uri);
            tx.success();
            return assocType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching association type \"" + uri + "\" failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Set<AssociationType> getAllAssociationTypes(ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            Set<AssociationType> assocTypes = new HashSet();
            for (String uri : getAssociationTypeUris()) {
                AssociationType assocType = getAssociationType(uri, clientState);
                assocTypes.add(assocType);
            }
            tx.success();
            return assocTypes;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Fetching all association types failed", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public AssociationType createAssociationType(AssociationTypeModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            objectFactory.storeType(model);
            AttachedAssociationType assocType = new AttachedAssociationType(model, this);
            typeCache.put(assocType);
            //
            tx.success();
            return assocType;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating association type \"" + model.getUri() + "\" failed (" + model + ")",
                e);
        } finally {
            tx.finish();
        }
    }



    // === Plugins ===

    @Override
    public Plugin getPlugin(String pluginUri) {
        return pluginManager.getPlugin(pluginUri);
    }

    @Override
    public Set<PluginInfo> getPluginInfo() {
        return pluginManager.getPluginInfo();
    }



    // === Access Control ===

    @Override
    public AccessControlList getACL(long objectId) {
        return storage.getACL(objectId);
    }

    @Override
    public void createACL(long objectId, AccessControlList acl) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.createACL(objectId, acl);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating access control list for object " + objectId + " failed", e);
        } finally {
            tx.finish();
        }
    }

    // ---

    @Override
    public String getCreator(long objectId) {
        return storage.getCreator(objectId);
    }

    @Override
    public void setCreator(long objectId, String username) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.setCreator(objectId, username);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting the creator of object " + objectId + " failed (username=\"" +
                username + "\")", e);
        } finally {
            tx.finish();
        }
    }

    // ---

    @Override
    public String getOwner(long objectId) {
        return storage.getOwner(objectId);
    }

    @Override
    public void setOwner(long objectId, String username) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            storage.setOwner(objectId, username);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting the owner of object " + objectId + " failed (username=\"" +
                username + "\")", e);
        } finally {
            tx.finish();
        }
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



    // *** End of DeepaMehtaService Implementation ***



    /**
     * Setups the database:
     *   1) initializes the database.
     *   2) in case of a clean install: sets up the bootstrap content.
     *   3) runs the core migrations.
     * <p>
     * Called from {@link CoreActivator#start}.
     */
    public void setupDB() {
        DeepaMehtaTransaction tx = beginTx();
        try {
            logger.info("----- Activating DeepaMehta 4 Core -----");
            boolean isCleanInstall = storage.init();
            if (isCleanInstall) {
                setupBootstrapContent();
            }
            migrationManager.runCoreMigrations(isCleanInstall);
            tx.success();
            tx.finish();
            logger.info("----- Activation of DeepaMehta 4 Core complete -----");
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            tx.finish();
            shutdown();
            throw new RuntimeException("Setting up the database failed", e);
        }
        // Note: we don't put finish() in a finally clause here because
        // in case of error the core service has to be shut down.
    }

    /**
     * Shuts down the database.
     * <p>
     * Called from {@link CoreActivator#stop}.
     */
    public void shutdown() {
        storage.shutdown();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Events ===

    List<Object> fireEvent(CoreEvent event, Object... params) {
        return listenerRegistry.fireEvent(event, params);
    }



    // === Helper ===

    Set<TopicModel> getTopicModels(Set<RelatedTopic> topics) {
        Set<TopicModel> models = new HashSet();
        for (Topic topic : topics) {
            models.add(((AttachedTopic) topic).getModel());
        }
        return models;
    }

    // ---

    /**
     * Convenience method.
     */
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return createAssociation(typeUri, roleModel1, roleModel2, null);    // ### FIXME: clientState=null
    }

    /**
     * Convenience method.
     */
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2, ClientState clientState) {
        return createAssociation(new AssociationModel(typeUri, roleModel1, roleModel2), clientState);
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
    AttachedAssociation attach(AssociationModel model, boolean fetchComposite) {
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

    AttachedRelatedAssociation attach(RelatedAssociationModel model, boolean fetchComposite,
                                                                     boolean fetchRelatingComposite) {
        if (fetchComposite || fetchRelatingComposite) {
            // ### TODO
            throw new RuntimeException("not yet implemented");
        }
        return new AttachedRelatedAssociation(model, this);
    }

    Set<RelatedAssociation> attach(Iterable<RelatedAssociationModel> models,
                                   boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedAssociation> relAssocs = new LinkedHashSet();
        for (RelatedAssociationModel model : models) {
            relAssocs.add(attach(model, fetchComposite, fetchRelatingComposite));
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
        objectFactory.associateWithTopicType(t.getId(), t.getTypeUri());
        objectFactory.associateWithTopicType(a.getId(), a.getTypeUri());
        objectFactory.associateWithTopicType(dataType.getId(), dataType.getTypeUri());
        objectFactory.associateWithTopicType(roleType.getId(), roleType.getTypeUri());
        objectFactory.associateWithTopicType(text.getId(), text.getTypeUri());
        objectFactory.associateWithTopicType(deflt.getId(), deflt.getTypeUri());
        objectFactory.associateWithTopicType(type.getId(), type.getTypeUri());
        objectFactory.associateWithTopicType(instance.getId(), instance.getTypeUri());
        objectFactory.associateWithTopicType(aggregation.getId(), aggregation.getTypeUri());
        objectFactory.associateWithTopicType(instantiation.getId(), instantiation.getTypeUri());
        //
        // 2) Postponed data type association
        //
        // Note: associateDataType() creates the association by a *high-level* (service) call.
        // This requires the association type (here: dm4.core.aggregation) to be fully constructed already.
        // That's why the topic type associations (step 1) must be performed *before* the data type associations.
        // ### FIXDOC: not true anymore
        //
        // Note: at time of the first associateDataType() call the required association type (dm4.core.aggregation)
        // is *not* fully constructed yet! (it gets constructed through this very call). This works anyway because
        // the data type assigning association is created *before* the association type is fetched.
        // (see AttachedAssociation.store(): storage.createAssociation() is called before getType()
        // in AttachedDeepaMehtaObject.store().)
        // ### FIXDOC: not true anymore
        //
        // Important is that associateDataType("dm4.core.aggregation") is the first call here.
        // ### FIXDOC: not true anymore
        //
        // Note: _associateDataType() creates the data type assigning association by a *low-level* (storage) call.
        // A high-level (service) call would fail while setting the association's value. The involved getType()
        // would fail (not because the association is missed -- it's created meanwhile, but)
        // because this involves fetching the association including its value. The value doesn't exist yet,
        // because its setting forms the begin of this vicious circle.
        _associateDataType("dm4.core.meta_type",  "dm4.core.text");
        _associateDataType("dm4.core.topic_type", "dm4.core.text");
        _associateDataType("dm4.core.assoc_type", "dm4.core.text");
        _associateDataType("dm4.core.data_type",  "dm4.core.text");
        _associateDataType("dm4.core.role_type",  "dm4.core.text");
        //
        _associateDataType("dm4.core.aggregation",   "dm4.core.text");
        _associateDataType("dm4.core.instantiation", "dm4.core.text");
    }

    private void _createTopic(TopicModel model) {
        // Note: low-level (storage) call used here ### explain
        storage.createTopic(model);
        storage.setTopicValue(model.getId(), model.getSimpleValue());
    }

    void _associateDataType(String typeUri, String dataTypeUri) {
        AssociationModel model = new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(typeUri,     "dm4.core.type"),
            new TopicRoleModel(dataTypeUri, "dm4.core.default"));
        storage.createAssociation(model);
        storage.setAssociationValue(model.getId(), model.getSimpleValue());
        objectFactory.associateWithAssociationType(model.getId(), model.getTypeUri());
    }

    private void bootstrapTypeCache() {
        typeCache.put(new AttachedTopicType(new TopicTypeModel("dm4.core.meta_meta_type",
            "dm4.core.meta_meta_meta_type", "Meta Meta Type", "dm4.core.text"), this));
    }
}
