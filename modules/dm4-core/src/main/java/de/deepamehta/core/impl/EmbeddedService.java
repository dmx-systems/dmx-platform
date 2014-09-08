package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.AssociationType;
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
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.TypeStorage;
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

    private static final String DEFAULT_TOPIC_TYPE_URI = "domain.project.topic_type_";
    private static final String DEFAULT_ASSOCIATION_TYPE_URI = "domain.project.assoc_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    StorageDecorator storageDecorator;
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
    public EmbeddedService(StorageDecorator storageDecorator, BundleContext bundleContext) {
        this.storageDecorator = storageDecorator;
        this.bundleContext = bundleContext;
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(this);
        this.eventManager = new EventManager(this);
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
    public ResultList<RelatedTopic> getTopics(String topicTypeUri, int maxResultSize) {
        try {
            return getTopicType(topicTypeUri).getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", topicTypeUri, maxResultSize);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed (topicTypeUri=\"" + topicTypeUri + "\")", e);
        }
    }

    @Override
    public List<Topic> searchTopics(String searchTerm, String fieldUri) {
        try {
            return instantiateTopics(storageDecorator.queryTopics(searchTerm, fieldUri));
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
        try {
            fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            Topic topic = topicFactory(model);
            fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic failed (" + model + ")", e);
        }
    }

    @Override
    public void updateTopic(TopicModel model) {
        try {
            // Note: the child topics are not needed for the actual update operation but for refreshing the label.
            // ### TODO: refactor labeling. Child topics involved in labeling should be loaded on demand.
            getTopic(model.getId()).loadChildTopics().update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic failed (" + model + ")", e);
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
        logger.info("assocId=" + assocId);
        try {
            return instantiateAssociation(storageDecorator.fetchAssociation(assocId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
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
    public List<RelatedAssociation> getAssociations(String assocTypeUri) {
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
            // Note: the child topics are not needed for the actual update operation but for refreshing the label.
            // ### TODO: refactor labeling. Child topics involved in labeling should be loaded on demand.
            getAssociation(model.getId()).loadChildTopics().update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating association failed (" + model + ")", e);
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
                "dm4.core.instance", "dm4.core.topic_type", 0);
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
            getTopicType(topicTypeUri).delete();
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
                "dm4.core.instance", "dm4.core.assoc_type", 0);
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
    public Object getDatabaseVendorObject() {
        return storageDecorator.getDatabaseVendorObject();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Helper ===

    void createTopicInstantiation(long topicId, String topicTypeUri) {
        try {
            AssociationModel assoc = new AssociationModel("dm4.core.instantiation",
                new TopicRoleModel(topicTypeUri, "dm4.core.type"),
                new TopicRoleModel(topicId, "dm4.core.instance"));
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
            AssociationModel assoc = new AssociationModel("dm4.core.instantiation",
                new TopicRoleModel(assocTypeUri, "dm4.core.type"),
                new AssociationRoleModel(assocId, "dm4.core.instance"));
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
        return createAssociation(new AssociationModel(typeUri, roleModel1, roleModel2));
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Attaches this core service to a topic model fetched from storage layer.
     */
    Topic instantiateTopic(TopicModel model) {
        return new AttachedTopic(model, this);
    }

    private List<Topic> instantiateTopics(List<TopicModel> models) {
        List<Topic> topics = new ArrayList();
        for (TopicModel model : models) {
            topics.add(instantiateTopic(model));
        }
        return topics;
    }

    // ---

    RelatedTopic instantiateRelatedTopic(RelatedTopicModel model) {
        return new AttachedRelatedTopic(model, this);
    }

    ResultList<RelatedTopic> instantiateRelatedTopics(ResultList<RelatedTopicModel> models) {
        List<RelatedTopic> relTopics = new ArrayList();
        for (RelatedTopicModel model : models) {
            relTopics.add(instantiateRelatedTopic(model));
        }
        return new ResultList<RelatedTopic>(models.getTotalCount(), relTopics);
    }

    // ===

    /**
     * Attaches this core service to an association fetched from storage layer.
     */
    Association instantiateAssociation(AssociationModel model) {
        return new AttachedAssociation(model, this);
    }

    List<Association> instantiateAssociations(List<AssociationModel> models) {
        List<Association> assocs = new ArrayList();
        for (AssociationModel model : models) {
            assocs.add(instantiateAssociation(model));
        }
        return assocs;
    }

    // ---

    RelatedAssociation instantiateRelatedAssociation(RelatedAssociationModel model) {
        return new AttachedRelatedAssociation(model, this);
    }

    List<RelatedAssociation> instantiateRelatedAssociations(Iterable<RelatedAssociationModel> models) {
        List<RelatedAssociation> relAssocs = new ArrayList();
        for (RelatedAssociationModel model : models) {
            relAssocs.add(instantiateRelatedAssociation(model));
        }
        return relAssocs;
    }

    // ===

    /**
     * Factory method: creates a new topic in the DB according to the given topic model and returns a topic instance.
     */
    private Topic topicFactory(TopicModel model) {
        // 1) store in DB
        storageDecorator.storeTopic(model);
        valueStorage.storeValue(model);
        createTopicInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) create application object
        return new AttachedTopic(model, this);
    }

    /**
     * Factory method: creates a new association in the DB according to the given association model and returns an
     * association instance.
     */
    private Association associationFactory(AssociationModel model) {
        // 1) store in DB
        storageDecorator.storeAssociation(model);
        valueStorage.storeValue(model);
        createAssociationInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) create application object
        return new AttachedAssociation(model, this);
    }

    // ---

    /**
     * Factory method: creates a new topic type in the DB according to the given topic type model
     * and returns a topic type instance.
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
     * Factory method: creates a new association type in the DB according to the given association type model
     * and returns a topic type instance.
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

    private void createTypeTopic(TopicModel model, String defaultUriPrefix) {
        Topic typeTopic = topicFactory(model);
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
            TopicModel t = new TopicModel("dm4.core.topic_type", "dm4.core.meta_type",
                new SimpleValue("Topic Type"));
            TopicModel a = new TopicModel("dm4.core.assoc_type", "dm4.core.meta_type",
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
        AssociationModel assoc = new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(typeUri,     "dm4.core.type"),
            new TopicRoleModel(dataTypeUri, "dm4.core.default"));
        storageDecorator.storeAssociation(assoc);
        storageDecorator.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
    }

    // ---

    private void bootstrapTypeCache() {
        typeCache.putTopicType(new AttachedTopicType(new TopicTypeModel("dm4.core.meta_meta_type",
            "dm4.core.meta_meta_meta_type", "Meta Meta Type", "dm4.core.text"), this));
    }
}
