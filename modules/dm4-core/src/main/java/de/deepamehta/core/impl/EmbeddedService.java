package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
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
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.TypeStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.osgi.framework.BundleContext;

import java.util.Collection;
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

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DEFAULT_TOPIC_TYPE_URI = "domain.project.topic_type_";
    private static final String DEFAULT_ASSOCIATION_TYPE_URI = "domain.project.assoc_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    StorageDecorator storage;
    BundleContext bundleContext;
    MigrationManager migrationManager;
    PluginManager pluginManager;
    EventManager eventManager;
    TypeCache typeCache;
    TypeStorageImpl typeStorage;
    ValueStorage valueStorage;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DeepaMehta 4 Core bundle.
     */
    public EmbeddedService(StorageDecorator storage, BundleContext bundleContext) {
        this.storage = storage;
        this.bundleContext = bundleContext;
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(this);
        this.eventManager = new EventManager();
        this.typeCache = new TypeCache(this);
        this.typeStorage = new TypeStorageImpl(this);
        this.valueStorage = new ValueStorage(this);
        bootstrapTypeCache();
        setupDB();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaService Implementation ***
    // ****************************************



    // === Topics ===

    @Override
    public Topic getTopic(long topicId, boolean fetchComposite) {
        try {
            return instantiateTopic(storage.fetchTopic(topicId), fetchComposite);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
        }
    }

    @Override
    public Topic getTopic(String key, SimpleValue value, boolean fetchComposite) {
        try {
            TopicModel topic = storage.fetchTopic(key, value);
            return topic != null ? instantiateTopic(topic, fetchComposite) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    @Override
    public Set<Topic> getTopics(String key, SimpleValue value, boolean fetchComposite) {
        try {
            return instantiateTopics(storage.fetchTopics(key, value), fetchComposite);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    @Override
    public ResultSet<RelatedTopic> getTopics(String topicTypeUri, boolean fetchComposite, int maxResultSize) {
        try {
            return getTopicType(topicTypeUri).getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", topicTypeUri, fetchComposite, false, maxResultSize);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed (topicTypeUri=\"" + topicTypeUri + "\")", e);
        }
    }

    @Override
    public Set<Topic> searchTopics(String searchTerm, String fieldUri) {
        try {
            // ### FIXME: fetchComposite=false, parameterize it
            return instantiateTopics(storage.queryTopics(searchTerm, fieldUri), false);
        } catch (Exception e) {
            throw new RuntimeException("Searching topics failed (searchTerm=\"" + searchTerm + "\", fieldUri=\"" +
                fieldUri + "\")", e);
        }
    }

    @Override
    public Iterable<Topic> getAllTopics() {
        return new TopicIterable(this);
    }

    // ---

    @Override
    public Topic createTopic(TopicModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            fireEvent(CoreEvent.PRE_CREATE_TOPIC, model, clientState);
            //
            Directives directives = new Directives();   // ### FIXME: directives are ignored
            Topic topic = topicFactory(model, clientState, directives);
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
            Topic topic = getTopic(model.getId(), true);    // fetchComposite=true
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
    public Directives deleteTopic(long topicId) {
        DeepaMehtaTransaction tx = beginTx();
        Topic topic = null;
        try {
            topic = getTopic(topicId, true);    // fetchComposite=true ### FIXME: false?
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
    public Association getAssociation(long assocId, boolean fetchComposite) {
        logger.info("assocId=" + assocId + ", fetchComposite=" + fetchComposite);
        try {
            return instantiateAssociation(storage.fetchAssociation(assocId), fetchComposite);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
        }
    }

    @Override
    public Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id,
                                                           String roleTypeUri1, String roleTypeUri2,
                                                           boolean fetchComposite) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\", fetchComposite=" +
            fetchComposite;
        try {
            AssociationModel assoc = storage.fetchAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1,
                roleTypeUri2);
            return assoc != null ? instantiateAssociation(assoc, fetchComposite) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    @Override
    public Association getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                String topicRoleTypeUri, String assocRoleTypeUri,
                                                                boolean fetchComposite) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topicId=" + topicId + ", assocId=" + assocId +
            ", topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri +
            "\", fetchComposite=" + fetchComposite;
        logger.info(info);
        try {
            AssociationModel assoc = storage.fetchAssociationBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
                topicRoleTypeUri, assocRoleTypeUri);
            return assoc != null ? instantiateAssociation(assoc, fetchComposite) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    // ---

    @Override
    public Set<RelatedAssociation> getAssociations(String assocTypeUri) {
        try {
            return getAssociationType(assocTypeUri).getRelatedAssociations("dm4.core.instantiation",
                "dm4.core.type", "dm4.core.instance", assocTypeUri, false, false);
                // fetchComposite=false, fetchRelatingComposite=false
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations by type failed (assocTypeUri=\"" + assocTypeUri + "\")",
                e);
        }
    }

    @Override
    public Set<Association> getAssociations(long topic1Id, long topic2Id) {
        return getAssociations(topic1Id, topic2Id, null);
    }

    @Override
    public Set<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        logger.info("topic1Id=" + topic1Id + ", topic2Id=" + topic2Id + ", assocTypeUri=\"" + assocTypeUri + "\"");
        try {
            // ### FIXME: fetchComposite=false, parameterize it
            return instantiateAssociations(storage.fetchAssociations(assocTypeUri, topic1Id, topic2Id, null, null),
                false);     // roleTypeUri1=null, roleTypeUri2=null
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations between topics " + topic1Id + " and " + topic2Id +
                " failed (assocTypeUri=\"" + assocTypeUri + "\")", e);
        }
    }

    // ---

    @Override
    public Iterable<Association> getAllAssociations() {
        return new AssociationIterable(this);
    }

    // ---

    @Override
    public Association createAssociation(AssociationModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model, clientState);
            //
            Directives directives = new Directives();   // ### FIXME: directives are ignored
            Association assoc = associationFactory(model, clientState, directives);
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
            Association assoc = getAssociation(model.getId(), true);    // fetchComposite=true
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
    public Directives deleteAssociation(long assocId) {
        DeepaMehtaTransaction tx = beginTx();
        Association assoc = null;
        try {
            assoc = getAssociation(assocId, false);     // fetchComposite=false
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
            Topic metaType = instantiateTopic(storage.fetchTopic("uri", new SimpleValue("dm4.core.topic_type")), false);
            ResultSet<RelatedTopic> topicTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.topic_type", false, false, 0);
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
    public TopicType getTopicType(String uri) {
        try {
            return typeCache.getTopicType(uri);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic type \"" + uri + "\" failed", e);
        }
    }

    @Override
    public Set<TopicType> getAllTopicTypes() {
        try {
            Set<TopicType> topicTypes = new HashSet();
            for (String uri : getTopicTypeUris()) {
                TopicType topicType = getTopicType(uri);
                topicTypes.add(topicType);
            }
            return topicTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all topic types failed", e);
        }
    }

    @Override
    public TopicType createTopicType(TopicTypeModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            TopicType topicType = topicTypeFactory(model);
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
            String topicTypeUri = getTopic(model.getId(), false).getUri();     // fetchComposite=false
            TopicType topicType = getTopicType(topicTypeUri);
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
            Topic metaType = instantiateTopic(storage.fetchTopic("uri", new SimpleValue("dm4.core.assoc_type")), false);
            ResultSet<RelatedTopic> assocTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.assoc_type", false, false, 0);
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
    public AssociationType getAssociationType(String uri) {
        try {
            return typeCache.getAssociationType(uri);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association type \"" + uri + "\" failed", e);
        }
    }

    @Override
    public Set<AssociationType> getAllAssociationTypes() {
        try {
            Set<AssociationType> assocTypes = new HashSet();
            for (String uri : getAssociationTypeUris()) {
                AssociationType assocType = getAssociationType(uri);
                assocTypes.add(assocType);
            }
            return assocTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all association types failed", e);
        }
    }

    @Override
    public AssociationType createAssociationType(AssociationTypeModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            AssociationType assocType = associationTypeFactory(model);
            //
            fireEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType, clientState);
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

    @Override
    public Directives updateAssociationType(AssociationTypeModel model, ClientState clientState) {
        DeepaMehtaTransaction tx = beginTx();
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            String assocTypeUri = getTopic(model.getId(), false).getUri();     // fetchComposite=false
            AssociationType assocType = getAssociationType(assocTypeUri);
            Directives directives = new Directives();
            //
            assocType.update(model, clientState, directives);
            //
            tx.success();
            return directives;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating association type failed (" + model + ")", e);
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



    // === Properties ===

    @Override
    public Collection<Topic> getTopicsByProperty(String propUri, Object propValue) {
        return instantiateTopics(storage.fetchTopicsByProperty(propUri, propValue), false);
            // fetchComposite=false
    }

    @Override
    public Collection<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return instantiateTopics(storage.fetchTopicsByPropertyRange(propUri, from, to), false);
            // fetchComposite=false
    }

    @Override
    public Collection<Association> getAssociationsByProperty(String propUri, Object propValue) {
        return instantiateAssociations(storage.fetchAssociationsByProperty(propUri, propValue), false);
            // fetchComposite=false
    }

    @Override
    public Collection<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return instantiateAssociations(storage.fetchAssociationsByPropertyRange(propUri, from, to), false);
            // fetchComposite=false
    }



    // === Misc ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return storage.beginTx();
    }

    @Override
    public TypeStorage getTypeStorage() {
        return typeStorage;
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Events ===

    void fireEvent(CoreEvent event, Object... params) {
        eventManager.fireEvent(event, params);
    }



    // === Helper ===

    void createTopicInstantiation(long topicId, String topicTypeUri) {
        try {
            AssociationModel assoc = new AssociationModel("dm4.core.instantiation",
                new TopicRoleModel(topicTypeUri, "dm4.core.type"),
                new TopicRoleModel(topicId, "dm4.core.instance"));
            storage.storeAssociation(assoc);    // direct storage calls used here ### explain
            storage.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
            createAssociationInstantiation(assoc.getId(), assoc.getTypeUri());
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId +
                " with topic type \"" + topicTypeUri + "\" failed", e);
        }
    }

    void createAssociationInstantiation(long assocId, String assocTypeUri) {
        try {
            AssociationModel assoc = new AssociationModel("dm4.core.instantiation",
                new TopicRoleModel(assocTypeUri, "dm4.core.type"),
                new AssociationRoleModel(assocId, "dm4.core.instance"));
            storage.storeAssociation(assoc);    // direct storage calls used here ### explain
            storage.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
        } catch (Exception e) {
            throw new RuntimeException("Associating association " + assocId +
                " with association type \"" + assocTypeUri + "\" failed", e);
        }
    }

    // ---

    /**
     * Convenience method. ### to be dropped?
     */
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return createAssociation(typeUri, roleModel1, roleModel2, null);    // ### FIXME: clientState=null
    }

    /**
     * Convenience method. ### to be dropped?
     */
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2, ClientState clientState) {
        return createAssociation(new AssociationModel(typeUri, roleModel1, roleModel2), clientState);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Attaches this core service to a topic model fetched from storage layer.
     * Optionally fetches the topic's composite value from storage layer.
     */
    Topic instantiateTopic(TopicModel model, boolean fetchComposite) {
        fetchCompositeValue(model, fetchComposite);
        return new AttachedTopic(model, this);
    }

    private Set<Topic> instantiateTopics(Collection<TopicModel> models, boolean fetchComposite) {
        Set<Topic> topics = new LinkedHashSet();
        for (TopicModel model : models) {
            topics.add(instantiateTopic(model, fetchComposite));
        }
        return topics;
    }

    // ---

    RelatedTopic instantiateRelatedTopic(RelatedTopicModel model, boolean fetchComposite,
                                                                  boolean fetchRelatingComposite) {
        fetchCompositeValue(model, fetchComposite, fetchRelatingComposite);
        return new AttachedRelatedTopic(model, this);
    }

    ResultSet<RelatedTopic> instantiateRelatedTopics(ResultSet<RelatedTopicModel> models,
                                                     boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedTopic> relTopics = new LinkedHashSet();
        for (RelatedTopicModel model : models) {
            relTopics.add(instantiateRelatedTopic(model, fetchComposite, fetchRelatingComposite));
        }
        return new ResultSet<RelatedTopic>(models.getTotalCount(), relTopics);
    }

    // ===

    /**
     * Attaches this core service to an association fetched from storage layer.
     * Optionally fetches the topic's composite value from storage layer.
     */
    Association instantiateAssociation(AssociationModel model, boolean fetchComposite) {
        fetchCompositeValue(model, fetchComposite);
        return new AttachedAssociation(model, this);
    }

    Set<Association> instantiateAssociations(Collection<AssociationModel> models, boolean fetchComposite) {
        Set<Association> assocs = new LinkedHashSet();
        for (AssociationModel model : models) {
            assocs.add(instantiateAssociation(model, fetchComposite));
        }
        return assocs;
    }

    // ---

    RelatedAssociation instantiateRelatedAssociation(RelatedAssociationModel model, boolean fetchComposite,
                                                                                    boolean fetchRelatingComposite) {
        if (fetchComposite || fetchRelatingComposite) {
            // ### TODO
            throw new RuntimeException("not yet implemented");
        }
        return new AttachedRelatedAssociation(model, this);
    }

    Set<RelatedAssociation> instantiateRelatedAssociations(Iterable<RelatedAssociationModel> models,
                                                           boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedAssociation> relAssocs = new LinkedHashSet();
        for (RelatedAssociationModel model : models) {
            relAssocs.add(instantiateRelatedAssociation(model, fetchComposite, fetchRelatingComposite));
        }
        return relAssocs;
    }

    // ===

    private void fetchCompositeValue(DeepaMehtaObjectModel model, boolean fetchComposite) {
        if (fetchComposite) {
            valueStorage.fetchCompositeValue(model);
        }
    }

    private void fetchCompositeValue(RelatedTopicModel model, boolean fetchComposite, boolean fetchRelatingComposite) {
        fetchCompositeValue(model, fetchComposite);
        if (fetchRelatingComposite) {
            valueStorage.fetchCompositeValue(model.getRelatingAssociation());
        }
    }

    // ---

    /**
     * Factory method.
     */
    private Topic topicFactory(TopicModel model, ClientState clientState, Directives directives) {
        // 1) store in DB
        setDefaults(model);
        storage.storeTopic(model);
        valueStorage.storeValue(model, clientState, directives);
        createTopicInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) create application object
        return new AttachedTopic(model, this);
    }

    /**
     * Factory method.
     */
    private Association associationFactory(AssociationModel model, ClientState clientState, Directives directives) {
        // 1) store in DB
        setDefaults(model);
        storage.storeAssociation(model);
        valueStorage.storeValue(model, clientState, directives);
        createAssociationInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) create application object
        return new AttachedAssociation(model, this);
    }

    // ---

    /**
     * Factory method.
     */
    private TopicType topicTypeFactory(TopicTypeModel model) {
        // 1) store in DB
        createTypeTopic(model, DEFAULT_TOPIC_TYPE_URI);         // store generic topic
        typeStorage.storeType(model);                           // store type-specific parts
        //
        // 2) create application object
        TopicType topicType = new AttachedTopicType(model, this);
        typeCache.putTopicType(topicType);
        //
        return topicType;
    }

    /**
     * Factory method.
     */
    private AssociationType associationTypeFactory(AssociationTypeModel model) {
        // 1) store in DB
        createTypeTopic(model, DEFAULT_ASSOCIATION_TYPE_URI);   // store generic topic
        typeStorage.storeType(model);                           // store type-specific parts
        //
        // 2) create application object
        AssociationType assocType = new AttachedAssociationType(model, this);
        typeCache.putAssociationType(assocType);
        //
        return assocType;
    }

    // ---

    // ### TODO: differentiate between a model and an update model and then drop this method
    private void setDefaults(DeepaMehtaObjectModel model) {
        if (model.getUri() == null) {
            model.setUri("");
        }
        if (model.getSimpleValue() == null) {
            model.setSimpleValue("");
        }
    }

    private void createTypeTopic(TopicModel model, String defaultUriPrefix) {
        Topic typeTopic = topicFactory(model, null, null);   // ### FIXME: clientState, directives
        // If no URI is set the type gets a default URI based on its ID.
        // Note: this must be done *after* the topic is created. The ID is not known before.
        if (typeTopic.getUri().equals("")) {
            typeTopic.setUri(defaultUriPrefix + typeTopic.getId());
        }
    }



    // === Bootstrap ===

    /**
     * Setups the database:
     *   1) initializes the database.
     *   2) in case of a clean install: sets up the bootstrap content.
     *   3) runs the core migrations.
     * <p>
     * Called from {@link CoreActivator#start}.
     */
    private void setupDB() {
        DeepaMehtaTransaction tx = beginTx();
        try {
            logger.info("----- Setting up the database -----");
            boolean isCleanInstall = storage.init();
            if (isCleanInstall) {
                setupBootstrapContent();
            }
            migrationManager.runCoreMigrations(isCleanInstall);
            tx.success();
            tx.finish();
            logger.info("----- Setting up the database complete -----");
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            // Note: we don't put finish() in a finally clause here because
            // in case of error the database has to be shut down.
            tx.finish();
            storage.shutdown();
            throw new RuntimeException("Setting up the database failed", e);
        }
    }

    private void setupBootstrapContent() {
        try {
            // Before topic types and asscociation types can be created the meta types must be created
            TopicModel t = new TopicModel("dm4.core.topic_type", "dm4.core.meta_type",
                new SimpleValue("Topic Type"));
            TopicModel a = new TopicModel("dm4.core.assoc_type", "dm4.core.meta_type",
                new SimpleValue("Association Type"));
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
            TopicModel deflt = new TopicModel("dm4.core.default",  "dm4.core.role_type", new SimpleValue("Default"));
            TopicModel type  = new TopicModel("dm4.core.type",     "dm4.core.role_type", new SimpleValue("Type"));
            TopicModel inst  = new TopicModel("dm4.core.instance", "dm4.core.role_type", new SimpleValue("Instance"));
            _createTopic(deflt);
            _createTopic(type);
            _createTopic(inst);
            // Create association type "Aggregation" -- needed to associate topic/association types with data types
            TopicModel aggregation = new AssociationTypeModel("dm4.core.aggregation", "Aggregation", "dm4.core.text");
            _createTopic(aggregation);
            // Create association type "Instantiation" -- needed to associate topics with topic types
            TopicModel instn = new AssociationTypeModel("dm4.core.instantiation", "Instantiation", "dm4.core.text");
            _createTopic(instn);
            //
            // 1) Postponed topic type association
            //
            // Note: createTopicInstantiation() creates the associations by *low-level* (storage) calls.
            // That's why the associations can be created *before* their type (here: "dm4.core.instantiation")
            // is fully constructed (the type's data type is not yet associated => step 2).
            createTopicInstantiation(t.getId(), t.getTypeUri());
            createTopicInstantiation(a.getId(), a.getTypeUri());
            createTopicInstantiation(dataType.getId(), dataType.getTypeUri());
            createTopicInstantiation(roleType.getId(), roleType.getTypeUri());
            createTopicInstantiation(text.getId(), text.getTypeUri());
            createTopicInstantiation(deflt.getId(), deflt.getTypeUri());
            createTopicInstantiation(type.getId(), type.getTypeUri());
            createTopicInstantiation(inst.getId(), inst.getTypeUri());
            createTopicInstantiation(aggregation.getId(), aggregation.getTypeUri());
            createTopicInstantiation(instn.getId(), instn.getTypeUri());
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
            // (see AttachedAssociation.store(): storage.storeAssociation() is called before getType()
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
            typeStorage._associateDataType("dm4.core.meta_type",  "dm4.core.text");
            typeStorage._associateDataType("dm4.core.topic_type", "dm4.core.text");
            typeStorage._associateDataType("dm4.core.assoc_type", "dm4.core.text");
            typeStorage._associateDataType("dm4.core.data_type",  "dm4.core.text");
            typeStorage._associateDataType("dm4.core.role_type",  "dm4.core.text");
            //
            typeStorage._associateDataType("dm4.core.aggregation",   "dm4.core.text");
            typeStorage._associateDataType("dm4.core.instantiation", "dm4.core.text");
        } catch (Exception e) {
            throw new RuntimeException("Setting up the bootstrap content failed", e);
        }
    }

    /**
     * A low-level call that stores a topic without its "Instantiation" association.
     * Used for bootstrapping.
     */
    private void _createTopic(TopicModel model) {
        storage.storeTopic(model);
        storage.storeTopicValue(model.getId(), model.getSimpleValue());
    }

    private void bootstrapTypeCache() {
        typeCache.putTopicType(new AttachedTopicType(new TopicTypeModel("dm4.core.meta_meta_type",
            "dm4.core.meta_meta_meta_type", "Meta Meta Type", "dm4.core.text"), this));
    }
}
