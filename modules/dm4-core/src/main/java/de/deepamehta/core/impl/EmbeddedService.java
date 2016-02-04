package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
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
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.TypeStorage;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



/**
 * Implementation of the DeepaMehta core service. Embeddable into Java applications.
 */
public class EmbeddedService implements DeepaMehtaService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String URI_PREFIX_TOPIC_TYPE       = "domain.project.topic_type_";
    private static final String URI_PREFIX_ASSOCIATION_TYPE = "domain.project.assoc_type_";
    private static final String URI_PREFIX_ROLE_TYPE        = "domain.project.role_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    StorageDecorator storageDecorator;
    BundleContext bundleContext;
    ModelFactory mf;
    MigrationManager migrationManager;
    PluginManager pluginManager;
    EventManager eventManager;
    TypeCache typeCache;
    TypeStorageImpl typeStorage;
    ValueStorage valueStorage;
    AccessControl accessControl;
    WebPublishingService wpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DeepaMehta 4 Core bundle.
     */
    public EmbeddedService(StorageDecorator storageDecorator, BundleContext bundleContext) {
        this.storageDecorator = storageDecorator;
        this.bundleContext = bundleContext;
        this.mf = new ModelFactoryImpl(storageDecorator);
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(this);
        this.eventManager = new EventManager(this);
        this.typeCache = new TypeCache(this);
        this.typeStorage = new TypeStorageImpl(this);
        this.valueStorage = new ValueStorage(this);
        this.accessControl = new AccessControlImpl(this);
        this.wpService = new WebPublishingService(this);
        //
        bootstrapTypeCache();
        setupDB();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaService Implementation ***
    // ****************************************



    // === Topics ===

    @Override
    public Topic getTopic(long topicId) {
        try {
            return instantiateTopic(storageDecorator.fetchTopic(topicId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
        }
    }

    @Override
    public Topic getTopic(String key, SimpleValue value) {
        try {
            TopicModel topic = storageDecorator.fetchTopic(key, value);
            return topic != null ? instantiateTopic(topic) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    @Override
    public List<Topic> getTopics(String key, SimpleValue value) {
        try {
            return instantiateTopics(storageDecorator.fetchTopics(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    @Override
    public ResultList<RelatedTopic> getTopics(String topicTypeUri) {
        try {
            return getTopicType(topicTypeUri).getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", topicTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed (topicTypeUri=\"" + topicTypeUri + "\")", e);
        }
    }

    @Override
    public List<Topic> searchTopics(String searchTerm, String fieldUri) {
        try {
            return instantiateTopics(storageDecorator.queryTopics(fieldUri, new SimpleValue(searchTerm)));
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
    public Topic createTopic(TopicModel model) {
        return createTopic(model, null);    // uriPrefix=null
    }

    @Override
    public void updateTopic(TopicModel model) {
        try {
            getTopic(model.getId()).update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    @Override
    public void deleteTopic(long topicId) {
        try {
            getTopic(topicId).delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting topic " + topicId + " failed", e);
        }
    }



    // === Associations ===

    @Override
    public Association getAssociation(long assocId) {
        try {
            return instantiateAssociation(storageDecorator.fetchAssociation(assocId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
        }
    }

    @Override
    public Association getAssociation(String key, SimpleValue value) {
        try {
            AssociationModel assoc = storageDecorator.fetchAssociation(key, value);
            return assoc != null ? instantiateAssociation(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    @Override
    public List<Association> getAssociations(String key, SimpleValue value) {
        try {
            return instantiateAssociations(storageDecorator.fetchAssociations(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching associationss failed (key=\"" + key + "\", value=\"" + value + "\")",
                e);
        }
    }

    @Override
    public Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id,
                                                           String roleTypeUri1, String roleTypeUri2) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"";
        try {
            AssociationModel assoc = storageDecorator.fetchAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1,
                roleTypeUri2);
            return assoc != null ? instantiateAssociation(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    @Override
    public Association getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                String topicRoleTypeUri, String assocRoleTypeUri) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topicId=" + topicId + ", assocId=" + assocId +
            ", topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\"";
        logger.info(info);
        try {
            AssociationModel assoc = storageDecorator.fetchAssociationBetweenTopicAndAssociation(assocTypeUri,
                topicId, assocId, topicRoleTypeUri, assocRoleTypeUri);
            return assoc != null ? instantiateAssociation(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    // ---

    @Override
    public ResultList<RelatedAssociation> getAssociations(String assocTypeUri) {
        try {
            return getAssociationType(assocTypeUri).getRelatedAssociations("dm4.core.instantiation",
                "dm4.core.type", "dm4.core.instance", assocTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations by type failed (assocTypeUri=\"" + assocTypeUri + "\")",
                e);
        }
    }

    @Override
    public List<Association> getAssociations(long topic1Id, long topic2Id) {
        return getAssociations(topic1Id, topic2Id, null);
    }

    @Override
    public List<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        logger.info("topic1Id=" + topic1Id + ", topic2Id=" + topic2Id + ", assocTypeUri=\"" + assocTypeUri + "\"");
        try {
            return instantiateAssociations(storageDecorator.fetchAssociations(assocTypeUri, topic1Id, topic2Id,
                null, null));     // roleTypeUri1=null, roleTypeUri2=null
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

    @Override
    public long[] getPlayerIds(long assocId) {
        return storageDecorator.fetchPlayerIds(assocId);
    }

    // ---

    @Override
    public Association createAssociation(AssociationModel model) {
        try {
            fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            Association assoc = associationFactory(model);
            fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, assoc);
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed (" + model + ")", e);
        }
    }

    @Override
    public void updateAssociation(AssociationModel model) {
        try {
            getAssociation(model.getId()).update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating association " + model.getId() + " failed (typeUri=\"" +
                model.getTypeUri() + "\")", e);
        }
    }

    @Override
    public void deleteAssociation(long assocId) {
        try {
            getAssociation(assocId).delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting association " + assocId + " failed", e);
        }
    }



    // === Topic Types ===

    @Override
    public List<String> getTopicTypeUris() {
        try {
            Topic metaType = instantiateTopic(storageDecorator.fetchTopic("uri",
                new SimpleValue("dm4.core.topic_type")));
            ResultList<RelatedTopic> topicTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.topic_type");
            List<String> topicTypeUris = new ArrayList();
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
    public List<TopicType> getAllTopicTypes() {
        try {
            List<TopicType> topicTypes = new ArrayList();
            for (String uri : getTopicTypeUris()) {
                TopicType topicType = getTopicType(uri);
                topicTypes.add(topicType);
            }
            return topicTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all topic types failed", e);
        }
    }

    // ---

    @Override
    public TopicType createTopicType(TopicTypeModel model) {
        try {
            TopicType topicType = topicTypeFactory(model);
            fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType);
            return topicType;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic type \"" + model.getUri() + "\" failed (" + model + ")", e);
        }
    }

    @Override
    public void updateTopicType(TopicTypeModel model) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            String topicTypeUri = getTopic(model.getId()).getUri();
            getTopicType(topicTypeUri).update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic type failed (" + model + ")", e);
        }
    }

    @Override
    public void deleteTopicType(String topicTypeUri) {
        try {
            getTopicType(topicTypeUri).delete();    // ### TODO: delete view config topics
        } catch (Exception e) {
            throw new RuntimeException("Deleting topic type \"" + topicTypeUri + "\" failed", e);
        }
    }



    // === Association Types ===

    @Override
    public List<String> getAssociationTypeUris() {
        try {
            Topic metaType = instantiateTopic(storageDecorator.fetchTopic("uri",
                new SimpleValue("dm4.core.assoc_type")));
            ResultList<RelatedTopic> assocTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.assoc_type");
            List<String> assocTypeUris = new ArrayList();
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
    public List<AssociationType> getAllAssociationTypes() {
        try {
            List<AssociationType> assocTypes = new ArrayList();
            for (String uri : getAssociationTypeUris()) {
                AssociationType assocType = getAssociationType(uri);
                assocTypes.add(assocType);
            }
            return assocTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all association types failed", e);
        }
    }

    // ---

    @Override
    public AssociationType createAssociationType(AssociationTypeModel model) {
        try {
            AssociationType assocType = associationTypeFactory(model);
            fireEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType);
            return assocType;
        } catch (Exception e) {
            throw new RuntimeException("Creating association type \"" + model.getUri() + "\" failed (" + model + ")",
                e);
        }
    }

    @Override
    public void updateAssociationType(AssociationTypeModel model) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            String assocTypeUri = getTopic(model.getId()).getUri();
            getAssociationType(assocTypeUri).update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating association type failed (" + model + ")", e);
        }
    }

    @Override
    public void deleteAssociationType(String assocTypeUri) {
        try {
            getAssociationType(assocTypeUri).delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting association type \"" + assocTypeUri + "\" failed", e);
        }
    }



    // === Role Types ===

    @Override
    public Topic createRoleType(TopicModel model) {
        // check type URI argument
        String typeUri = model.getTypeUri();
        if (typeUri == null) {
            model.setTypeUri("dm4.core.role_type");
        } else {
            if (!typeUri.equals("dm4.core.role_type")) {
                throw new IllegalArgumentException("A role type is supposed to be of type \"dm4.core.role_type\" " +
                    "(found: \"" + typeUri + "\")");
            }
        }
        //
        return createTopic(model, URI_PREFIX_ROLE_TYPE);
    }



    // === Generic Object ===

    @Override
    public DeepaMehtaObject getObject(long id) {
        DeepaMehtaObjectModel model = storageDecorator.fetchObject(id);
        if (model instanceof TopicModel) {
            return instantiateTopic((TopicModel) model);
        } else if (model instanceof AssociationModel) {
            return instantiateAssociation((AssociationModel) model);
        } else {
            throw new RuntimeException("Unexpected model: " + model);
        }
    }



    // === Plugins ===

    @Override
    public Plugin getPlugin(String pluginUri) {
        return pluginManager.getPlugin(pluginUri);
    }

    @Override
    public List<PluginInfo> getPluginInfo() {
        return pluginManager.getPluginInfo();
    }



    // === Events ===

    @Override
    public void fireEvent(DeepaMehtaEvent event, Object... params) {
        eventManager.fireEvent(event, params);
    }

    @Override
    public void deliverEvent(String pluginUri, DeepaMehtaEvent event, Object... params) {
        eventManager.deliverEvent(pluginUri, event, params);
    }



    // === Properties ===

    @Override
    public Object getProperty(long id, String propUri) {
        return storageDecorator.fetchProperty(id, propUri);
    }

    @Override
    public boolean hasProperty(long id, String propUri) {
        return storageDecorator.hasProperty(id, propUri);
    }

    // ---

    @Override
    public List<Topic> getTopicsByProperty(String propUri, Object propValue) {
        return instantiateTopics(storageDecorator.fetchTopicsByProperty(propUri, propValue));
    }

    @Override
    public List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return instantiateTopics(storageDecorator.fetchTopicsByPropertyRange(propUri, from, to));
    }

    @Override
    public List<Association> getAssociationsByProperty(String propUri, Object propValue) {
        return instantiateAssociations(storageDecorator.fetchAssociationsByProperty(propUri, propValue));
    }

    @Override
    public List<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return instantiateAssociations(storageDecorator.fetchAssociationsByPropertyRange(propUri, from, to));
    }

    // ---

    @Override
    public void addTopicPropertyIndex(String propUri) {
        int topics = 0;
        int added = 0;
        logger.info("########## Adding topic property index for \"" + propUri + "\"");
        for (Topic topic : getAllTopics()) {
            if (topic.hasProperty(propUri)) {
                Object value = topic.getProperty(propUri);
                storageDecorator.indexTopicProperty(topic.getId(), propUri, value);
                added++;
            }
            topics++;
        }
        logger.info("########## Adding topic property index complete\n    Topics processed: " + topics +
            "\n    added to index: " + added);
    }

    @Override
    public void addAssociationPropertyIndex(String propUri) {
        int assocs = 0;
        int added = 0;
        logger.info("########## Adding association property index for \"" + propUri + "\"");
        for (Association assoc : getAllAssociations()) {
            if (assoc.hasProperty(propUri)) {
                Object value = assoc.getProperty(propUri);
                storageDecorator.indexAssociationProperty(assoc.getId(), propUri, value);
                added++;
            }
            assocs++;
        }
        logger.info("########## Adding association property complete\n    Associations processed: " + assocs +
            "\n    added to index: " + added);
    }



    // === Misc ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return storageDecorator.beginTx();
    }

    @Override
    public TypeStorage getTypeStorage() {
        return typeStorage;
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }

    @Override
    public Object getDatabaseVendorObject() {
        return storageDecorator.getDatabaseVendorObject();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Helper ===

    void createTopicInstantiation(long topicId, String topicTypeUri) {
        try {
            AssociationModel assoc = mf.newAssociationModel("dm4.core.instantiation",
                mf.newTopicRoleModel(topicTypeUri, "dm4.core.type"),
                mf.newTopicRoleModel(topicId, "dm4.core.instance"));
            storageDecorator.storeAssociation(assoc);   // direct storage calls used here ### explain
            storageDecorator.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
            createAssociationInstantiation(assoc.getId(), assoc.getTypeUri());
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId +
                " with topic type \"" + topicTypeUri + "\" failed", e);
        }
    }

    void createAssociationInstantiation(long assocId, String assocTypeUri) {
        try {
            AssociationModel assoc = mf.newAssociationModel("dm4.core.instantiation",
                mf.newTopicRoleModel(assocTypeUri, "dm4.core.type"),
                mf.newAssociationRoleModel(assocId, "dm4.core.instance"));
            storageDecorator.storeAssociation(assoc);   // direct storage calls used here ### explain
            storageDecorator.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
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
        return createAssociation(mf.newAssociationModel(typeUri, roleModel1, roleModel2));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Instantiation ===

    /**
     * Attaches this core service to a topic model fetched from storage layer.
     */
    Topic instantiateTopic(TopicModel model) {
        checkAccess(model);
        return new AttachedTopic(model, this);
    }

    private List<Topic> instantiateTopics(List<TopicModel> models) {
        List<Topic> topics = new ArrayList();
        for (TopicModel model : models) {
            try {
                topics.add(instantiateTopic(model));
            } catch (AccessControlException e) {
                // don't add topic to result and continue
            }
        }
        return topics;
    }

    // ---

    RelatedTopic instantiateRelatedTopic(RelatedTopicModel model) {
        checkAccess(model);
        return new AttachedRelatedTopic(model, this);
    }

    ResultList<RelatedTopic> instantiateRelatedTopics(ResultList<RelatedTopicModel> models) {
        List<RelatedTopic> relTopics = new ArrayList();
        for (RelatedTopicModel model : models) {
            try {
                relTopics.add(instantiateRelatedTopic(model));
            } catch (AccessControlException e) {
                // don't add topic to result and continue
            }
        }
        return new ResultList<RelatedTopic>(relTopics);
    }

    // ===

    /**
     * Attaches this core service to an association model fetched from storage layer.
     */
    Association instantiateAssociation(AssociationModel model) {
        checkAccess(model);
        return new AttachedAssociation(model, this);
    }

    List<Association> instantiateAssociations(List<AssociationModel> models) {
        List<Association> assocs = new ArrayList();
        for (AssociationModel model : models) {
            try {
                assocs.add(instantiateAssociation(model));
            } catch (AccessControlException e) {
                // don't add association to result and continue
            }
        }
        return assocs;
    }

    // ---

    RelatedAssociation instantiateRelatedAssociation(RelatedAssociationModel model) {
        checkAccess(model);
        return new AttachedRelatedAssociation(model, this);
    }

    ResultList<RelatedAssociation> instantiateRelatedAssociations(Iterable<RelatedAssociationModel> models) {
        ResultList<RelatedAssociation> relAssocs = new ResultList();
        for (RelatedAssociationModel model : models) {
            try {
                relAssocs.add(instantiateRelatedAssociation(model));
            } catch (AccessControlException e) {
                // don't add association to result and continue
            }
        }
        return relAssocs;
    }



    // === Factory ===

    private Topic createTopic(TopicModel model, String uriPrefix) {
        try {
            fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            Topic topic = topicFactory(model, uriPrefix);
            fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    // ---

    private void checkAccess(TopicModel model) {
        fireEvent(CoreEvent.PRE_GET_TOPIC, model.getId());          // throws AccessControlException
    }

    private void checkAccess(AssociationModel model) {
        fireEvent(CoreEvent.PRE_GET_ASSOCIATION, model.getId());    // throws AccessControlException
    }

    // ---

    /**
     * Factory method: creates a new topic in the DB according to the given model
     * and returns a topic instance.
     */
    private Topic topicFactory(TopicModel model, String uriPrefix) {
        // 1) store in DB
        storageDecorator.storeTopic(model);
        valueStorage.storeValue(model);
        createTopicInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) instantiate
        Topic topic = new AttachedTopic(model, this);
        //
        // 3) set default URI
        // If no URI is given the topic gets a default URI based on its ID, if requested.
        // Note: this must be done *after* the topic is stored. The ID is not known before.
        // Note: in case no URI was given: once stored a topic's URI is empty (not null).
        if (uriPrefix != null && topic.getUri().equals("")) {
            topic.setUri(uriPrefix + topic.getId());
        }
        //
        return topic;
    }

    /**
     * Factory method: creates a new association in the DB according to the given model
     * and returns an association instance.
     */
    Association associationFactory(AssociationModel model) {
        // 1) store in DB
        storageDecorator.storeAssociation(model);
        valueStorage.storeValue(model);
        createAssociationInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) instantiate
        return new AttachedAssociation(model, this);
    }

    // ---

    /**
     * Factory method: creates a new topic type in the DB according to the given model
     * and returns a topic type instance.
     */
    private TopicType topicTypeFactory(TopicTypeModel model) {
        // 1) store in DB
        createTopic(model, URI_PREFIX_TOPIC_TYPE);          // store generic topic
        typeStorage.storeType(model);                       // store type-specific parts
        //
        // 2) instantiate
        TopicType topicType = new AttachedTopicType(model, this);
        typeCache.putTopicType(topicType);
        //
        return topicType;
    }

    /**
     * Factory method: creates a new association type in the DB according to the given model
     * and returns an association type instance.
     */
    private AssociationType associationTypeFactory(AssociationTypeModel model) {
        // 1) store in DB
        createTopic(model, URI_PREFIX_ASSOCIATION_TYPE);    // store generic topic
        typeStorage.storeType(model);                       // store type-specific parts
        //
        // 2) instantiate
        AssociationType assocType = new AttachedAssociationType(model, this);
        typeCache.putAssociationType(assocType);
        //
        return assocType;
    }



    // === Bootstrap ===

    /**
     * Setups the database:
     *   1) initializes the database.
     *   2) in case of a clean install: sets up the bootstrap content.
     *   3) runs the core migrations.
     */
    private void setupDB() {
        DeepaMehtaTransaction tx = beginTx();
        try {
            logger.info("----- Setting up the database -----");
            boolean isCleanInstall = storageDecorator.init();
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
            storageDecorator.shutdown();
            throw new RuntimeException("Setting up the database failed", e);
        }
    }

    private void setupBootstrapContent() {
        try {
            // Create meta types "Topic Type" and "Association Type" -- needed to create topic types and
            // asscociation types
            TopicModel t = mf.newTopicModel("dm4.core.topic_type", "dm4.core.meta_type",
                new SimpleValue("Topic Type"));
            TopicModel a = mf.newTopicModel("dm4.core.assoc_type", "dm4.core.meta_type",
                new SimpleValue("Association Type"));
            _createTopic(t);
            _createTopic(a);
            // Create topic types "Data Type" and "Role Type"
            // ### Note: the topic type "Data Type" depends on the data type "Text" and the data type "Text" in turn
            // depends on the topic type "Data Type". To resolve this circle we use a low-level (storage) call here
            // and postpone the data type association.
            TopicModel dataType = new TopicTypeModel("dm4.core.data_type", "Data Type", "dm4.core.text");
            TopicModel roleType = new TopicTypeModel("dm4.core.role_type", "Role Type", "dm4.core.text");
            _createTopic(dataType);
            _createTopic(roleType);
            // Create data type "Text"
            TopicModel text = mf.newTopicModel("dm4.core.text", "dm4.core.data_type", new SimpleValue("Text"));
            _createTopic(text);
            // Create role types "Default", "Type", and "Instance"
            TopicModel deflt = mf.newTopicModel("dm4.core.default",  "dm4.core.role_type", new SimpleValue("Default"));
            TopicModel type  = mf.newTopicModel("dm4.core.type",     "dm4.core.role_type", new SimpleValue("Type"));
            TopicModel inst  = mf.newTopicModel("dm4.core.instance", "dm4.core.role_type", new SimpleValue("Instance"));
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
            _associateDataType("dm4.core.meta_type",  "dm4.core.text");
            _associateDataType("dm4.core.topic_type", "dm4.core.text");
            _associateDataType("dm4.core.assoc_type", "dm4.core.text");
            _associateDataType("dm4.core.data_type",  "dm4.core.text");
            _associateDataType("dm4.core.role_type",  "dm4.core.text");
            //
            _associateDataType("dm4.core.aggregation",   "dm4.core.text");
            _associateDataType("dm4.core.instantiation", "dm4.core.text");
        } catch (Exception e) {
            throw new RuntimeException("Setting up the bootstrap content failed", e);
        }
    }

    // ---

    /**
     * Low-level method that stores a topic without its "Instantiation" association.
     * Needed for bootstrapping.
     */
    private void _createTopic(TopicModel model) {
        storageDecorator.storeTopic(model);
        storageDecorator.storeTopicValue(model.getId(), model.getSimpleValue());
    }

    /**
     * Low-level method that stores an (data type) association without its "Instantiation" association.
     * Needed for bootstrapping.
     */
    private void _associateDataType(String typeUri, String dataTypeUri) {
        AssociationModel assoc = mf.newAssociationModel("dm4.core.aggregation",
            mf.newTopicRoleModel(typeUri,     "dm4.core.type"),
            mf.newTopicRoleModel(dataTypeUri, "dm4.core.default"));
        storageDecorator.storeAssociation(assoc);
        storageDecorator.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
    }

    // ---

    private void bootstrapTypeCache() {
        TopicTypeModel metaMetaType = new TopicTypeModel("dm4.core.meta_meta_type", "Meta Meta Type", "dm4.core.text");
        metaMetaType.setTypeUri("dm4.core.meta_meta_meta_type");
        typeCache.putTopicType(new AttachedTopicType(metaMetaType, this));
    }
}
