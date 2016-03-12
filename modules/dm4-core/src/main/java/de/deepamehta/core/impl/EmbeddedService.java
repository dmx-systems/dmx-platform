package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



/**
 * Implementation of the DeepaMehta core service. Embeddable into Java applications.
 */
public class EmbeddedService implements DeepaMehtaService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    BundleContext bundleContext;
    PersistenceLayer pl;
    EventManager em;
    ModelFactory mf;
    MigrationManager migrationManager;
    PluginManager pluginManager;
    AccessControl accessControl;
    WebPublishingService wpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DeepaMehta 4 Core bundle.
     */
    public EmbeddedService(PersistenceLayer pl, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.pl = pl;
        this.em = pl.em;
        this.mf = pl.mf;
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(this);
        this.accessControl = new AccessControlImpl(pl);
        this.wpService = new WebPublishingService(pl);
        //
        setupDB();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaService Implementation ***
    // ****************************************



    // === Topics ===

    @Override
    public Topic getTopic(long topicId) {
        return pl.getTopic(topicId);
    }

    @Override
    public Topic getTopic(String key, SimpleValue value) {
        return pl.getTopic(key, value);
    }

    @Override
    public List<Topic> getTopics(String key, SimpleValue value) {
        return pl.getTopics(key, value);
    }

    @Override
    public List<Topic> getTopics(String topicTypeUri) {
        return pl.getTopics(topicTypeUri);
    }

    @Override
    public List<Topic> searchTopics(String searchTerm, String fieldUri) {
        return pl.searchTopics(searchTerm, fieldUri);
    }

    @Override
    public Iterable<Topic> getAllTopics() {
        return pl.getAllTopics();
    }

    // ---

    @Override
    public Topic createTopic(TopicModel model) {
        return pl.createTopic(model);
    }

    @Override
    public void updateTopic(TopicModel newModel) {
        pl.updateTopic(newModel);
    }

    @Override
    public void deleteTopic(long topicId) {
        pl.deleteTopic(topicId);
    }



    // === Associations ===

    @Override
    public Association getAssociation(long assocId) {
        return pl.getAssociation(assocId);
    }

    @Override
    public Association getAssociation(String key, SimpleValue value) {
        return pl.getAssociation(key, value);
    }

    @Override
    public List<Association> getAssociations(String key, SimpleValue value) {
        return pl.getAssociations(key, value);
    }

    @Override
    public Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id,
                                                           String roleTypeUri1, String roleTypeUri2) {
        return pl.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
    }

    @Override
    public Association getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                String topicRoleTypeUri, String assocRoleTypeUri) {
        return pl.getAssociationBetweenTopicAndAssociation(assocTypeUri, topicId, assocId, topicRoleTypeUri,
            assocRoleTypeUri);
    }

    // ---

    @Override
    public List<Association> getAssociations(String assocTypeUri) {
        return pl.getAssociations(assocTypeUri);
    }

    @Override
    public List<Association> getAssociations(long topic1Id, long topic2Id) {
        return pl.getAssociations(topic1Id, topic2Id);
    }

    @Override
    public List<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        return pl.getAssociations(topic1Id, topic2Id, assocTypeUri);
    }

    // ---

    @Override
    public Iterable<Association> getAllAssociations() {
        return pl.getAllAssociations();
    }

    @Override
    public long[] getPlayerIds(long assocId) {
        return pl.getPlayerIds(assocId);
    }

    // ---

    @Override
    public Association createAssociation(AssociationModel model) {
        return pl.createAssociation((AssociationModelImpl) model);
    }

    @Override
    public void updateAssociation(AssociationModel newModel) {
        pl.updateAssociation(newModel);
    }

    @Override
    public void deleteAssociation(long assocId) {
        pl.deleteAssociation(assocId);
    }



    // === Topic Types ===

    @Override
    public List<String> getTopicTypeUris() {
        try {
            Topic metaType = pl.checkReadAccessAndInstantiate(pl.fetchTopic("uri",
                new SimpleValue("dm4.core.topic_type")));       // ### TODO: rethink access control    
            ResultList<RelatedTopic> topicTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.topic_type");    // ### TODO: perform by-value search instead
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
        return pl.getTopicType(uri);
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
        return pl.createTopicType((TopicTypeModelImpl) model);
    }

    @Override
    public void updateTopicType(TopicTypeModel newModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            // ### FIXME: access control
            String topicTypeUri = pl.fetchTopic(newModel.getId()).getUri();
            pl.typeStorage.getTopicType(topicTypeUri).update(newModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic type failed (" + newModel + ")", e);
        }
    }

    @Override
    public void deleteTopicType(String topicTypeUri) {
        try {
            pl.typeStorage.getTopicType(topicTypeUri).delete();     // ### TODO: delete view config topics
        } catch (Exception e) {
            throw new RuntimeException("Deleting topic type \"" + topicTypeUri + "\" failed", e);
        }
    }



    // === Association Types ===

    @Override
    public List<String> getAssociationTypeUris() {
        try {
            Topic metaType = pl.checkReadAccessAndInstantiate(pl.fetchTopic("uri",
                new SimpleValue("dm4.core.assoc_type")));           // ### TODO: rethink access control
            ResultList<RelatedTopic> assocTypes = metaType.getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", "dm4.core.assoc_type");        // ### TODO: perform by-value search instead
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
        return pl.getAssociationType(uri);
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
        return pl.createAssociationType((AssociationTypeModelImpl) model);
    }

    @Override
    public void updateAssociationType(AssociationTypeModel newModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            // ### FIXME: access control
            String assocTypeUri = pl.fetchTopic(newModel.getId()).getUri();
            pl.typeStorage.getAssociationType(assocTypeUri).update(newModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating association type failed (" + newModel + ")", e);
        }
    }

    @Override
    public void deleteAssociationType(String assocTypeUri) {
        try {
            pl.typeStorage.getAssociationType(assocTypeUri).delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting association type \"" + assocTypeUri + "\" failed", e);
        }
    }



    // === Role Types ===

    @Override
    public Topic createRoleType(TopicModel model) {
        return pl.createRoleType(model);
    }



    // === Generic Object ===

    @Override
    public DeepaMehtaObject getObject(long id) {
        return pl.getObject(id);
    }



    // === Plugins ===

    @Override
    public PluginImpl getPlugin(String pluginUri) {
        return pluginManager.getPlugin(pluginUri);
    }

    @Override
    public List<PluginInfo> getPluginInfo() {
        return pluginManager.getPluginInfo();
    }



    // === Events ===

    @Override
    public void fireEvent(DeepaMehtaEvent event, Object... params) {
        em.fireEvent(event, params);
    }

    @Override
    public void deliverEvent(String pluginUri, DeepaMehtaEvent event, Object... params) {
        em.deliverEvent(getPlugin(pluginUri), event, params);
    }



    // === Properties ===

    @Override
    public Object getProperty(long id, String propUri) {
        return pl.fetchProperty(id, propUri);
    }

    @Override
    public boolean hasProperty(long id, String propUri) {
        return pl.hasProperty(id, propUri);
    }

    // ---

    @Override
    public List<Topic> getTopicsByProperty(String propUri, Object propValue) {
        return pl.getTopicsByProperty(propUri, propValue);
    }

    @Override
    public List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return pl.getTopicsByPropertyRange(propUri, from, to);
    }

    @Override
    public List<Association> getAssociationsByProperty(String propUri, Object propValue) {
        return pl.getAssociationsByProperty(propUri, propValue);
    }

    @Override
    public List<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return pl.getAssociationsByPropertyRange(propUri, from, to);
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
                pl.indexTopicProperty(topic.getId(), propUri, value);
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
                pl.indexAssociationProperty(assoc.getId(), propUri, value);
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
        return pl.beginTx();
    }

    // ---

    @Override
    public ModelFactory getModelFactory() {
        return mf;
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }

    @Override
    public Object getDatabaseVendorObject() {
        return pl.getDatabaseVendorObject();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Helper ===

    /**
     * Convenience method.
     */
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return createAssociation(mf.newAssociationModel(typeUri, roleModel1, roleModel2));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



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
            boolean isCleanInstall = pl.init();
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
            pl.shutdown();
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
            TopicModel dataType = mf.newTopicTypeModel("dm4.core.data_type", "Data Type", "dm4.core.text");
            TopicModel roleType = mf.newTopicTypeModel("dm4.core.role_type", "Role Type", "dm4.core.text");
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
            TopicModel aggregation = mf.newAssociationTypeModel("dm4.core.aggregation", "Aggregation", "dm4.core.text");
            _createTopic(aggregation);
            // Create association type "Instantiation" -- needed to associate topics with topic types
            TopicModel instn = mf.newAssociationTypeModel("dm4.core.instantiation", "Instantiation", "dm4.core.text");
            _createTopic(instn);
            //
            // 1) Postponed topic type association
            //
            // Note: createTopicInstantiation() creates the associations by *low-level* (storage) calls.
            // That's why the associations can be created *before* their type (here: "dm4.core.instantiation")
            // is fully constructed (the type's data type is not yet associated => step 2).
            pl.createTopicInstantiation(t.getId(), t.getTypeUri());
            pl.createTopicInstantiation(a.getId(), a.getTypeUri());
            pl.createTopicInstantiation(dataType.getId(), dataType.getTypeUri());
            pl.createTopicInstantiation(roleType.getId(), roleType.getTypeUri());
            pl.createTopicInstantiation(text.getId(), text.getTypeUri());
            pl.createTopicInstantiation(deflt.getId(), deflt.getTypeUri());
            pl.createTopicInstantiation(type.getId(), type.getTypeUri());
            pl.createTopicInstantiation(inst.getId(), inst.getTypeUri());
            pl.createTopicInstantiation(aggregation.getId(), aggregation.getTypeUri());
            pl.createTopicInstantiation(instn.getId(), instn.getTypeUri());
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
            // (see AssociationImpl.store(): storage.storeAssociation() is called before getType()
            // in DeepaMehtaObjectImpl.store().)
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
        pl.storeTopic(model);
        pl.storeTopicValue(model.getId(), model.getSimpleValue());
    }

    /**
     * Low-level method that stores an (data type) association without its "Instantiation" association.
     * Needed for bootstrapping.
     */
    private void _associateDataType(String typeUri, String dataTypeUri) {
        AssociationModel assoc = mf.newAssociationModel("dm4.core.aggregation",
            mf.newTopicRoleModel(typeUri,     "dm4.core.type"),
            mf.newTopicRoleModel(dataTypeUri, "dm4.core.default"));
        pl.storeAssociation(assoc);
        pl.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
    }
}
