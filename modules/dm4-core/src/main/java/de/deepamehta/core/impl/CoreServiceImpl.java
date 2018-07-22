package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DMXObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.service.DMXEvent;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



/**
 * Implementation of the DeepaMehta core service. Embeddable into Java applications.
 */
public class CoreServiceImpl implements CoreService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    BundleContext bundleContext;
    PersistenceLayer pl;
    EventManager em;
    ModelFactory mf;
    MigrationManager migrationManager;
    PluginManager pluginManager;
    AccessControl accessControl;
    WebSocketsServiceImpl wsService;
    WebPublishingService wpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DeepaMehta 4 Core bundle.
     */
    public CoreServiceImpl(PersistenceLayer pl, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.pl = pl;
        this.em = pl.em;
        this.mf = pl.mf;
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(this);
        this.accessControl = new AccessControlImpl(pl);
        this.wsService = new WebSocketsServiceImpl(this);
        this.wpService = new WebPublishingService(pl, wsService);
        //
        setupDB();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** CoreService Implementation ***
    // **********************************



    // === Topics ===

    @Override
    public Topic getTopic(long topicId) {
        return pl.getTopic(topicId);
    }

    @Override
    public TopicImpl getTopicByUri(String uri) {
        return pl.getTopicByUri(uri);
    }

    @Override
    public Topic getTopicByValue(String key, SimpleValue value) {
        return pl.getTopicByValue(key, value);
    }

    @Override
    public List<Topic> getTopicsByValue(String key, SimpleValue value) {
        return pl.getTopicsByValue(key, value);
    }

    @Override
    public List<Topic> getTopicsByType(String topicTypeUri) {
        return pl.getTopicsByType(topicTypeUri);
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
    public TopicImpl createTopic(TopicModel model) {
        return pl.createTopic((TopicModelImpl) model);
    }

    @Override
    public void updateTopic(TopicModel updateModel) {
        pl.updateTopic((TopicModelImpl) updateModel);
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
    public Association getAssociationByValue(String key, SimpleValue value) {
        return pl.getAssociationByValue(key, value);
    }

    @Override
    public List<Association> getAssociationsByValue(String key, SimpleValue value) {
        return pl.getAssociationsByValue(key, value);
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
    public List<Association> getAssociationsByType(String assocTypeUri) {
        return pl.getAssociationsByType(assocTypeUri);
    }

    @Override
    public List<Association> getAssociations(long topic1Id, long topic2Id) {
        return pl.getAssociations(topic1Id, topic2Id);
    }

    @Override
    public List<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        return pl.getAssociations(assocTypeUri, topic1Id, topic2Id);
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
    public AssociationImpl createAssociation(AssociationModel model) {
        return pl.createAssociation((AssociationModelImpl) model);
    }

    @Override
    public void updateAssociation(AssociationModel updateModel) {
        pl.updateAssociation((AssociationModelImpl) updateModel);
    }

    @Override
    public void deleteAssociation(long assocId) {
        pl.deleteAssociation(assocId);
    }



    // === Topic Types ===

    @Override
    public TopicTypeImpl getTopicType(String uri) {
        return pl.getTopicType(uri);
    }

    @Override
    public TopicTypeImpl getTopicTypeImplicitly(long topicId) {
        return pl.getTopicTypeImplicitly(topicId);
    }

    // ---

    @Override
    public List<TopicType> getAllTopicTypes() {
        return pl.getAllTopicTypes();
    }

    // ---

    @Override
    public TopicTypeImpl createTopicType(TopicTypeModel model) {
        return pl.createTopicType((TopicTypeModelImpl) model);
    }

    @Override
    public void updateTopicType(TopicTypeModel updateModel) {
        pl.updateTopicType((TopicTypeModelImpl) updateModel);
    }

    @Override
    public void deleteTopicType(String topicTypeUri) {
        pl.deleteTopicType(topicTypeUri);
    }



    // === Association Types ===

    @Override
    public AssociationTypeImpl getAssociationType(String uri) {
        return pl.getAssociationType(uri);
    }

    @Override
    public AssociationTypeImpl getAssociationTypeImplicitly(long assocId) {
        return pl.getAssociationTypeImplicitly(assocId);
    }

    // ---

    @Override
    public List<AssociationType> getAllAssociationTypes() {
        return pl.getAllAssociationTypes();
    }

    // ---

    @Override
    public AssociationTypeImpl createAssociationType(AssociationTypeModel model) {
        return pl.createAssociationType((AssociationTypeModelImpl) model);
    }

    @Override
    public void updateAssociationType(AssociationTypeModel updateModel) {
        pl.updateAssociationType((AssociationTypeModelImpl) updateModel);
    }

    @Override
    public void deleteAssociationType(String assocTypeUri) {
        pl.deleteAssociationType(assocTypeUri);
    }



    // === Role Types ===

    @Override
    public Topic createRoleType(TopicModel model) {
        return pl.createRoleType((TopicModelImpl) model);
    }



    // === Generic Object ===

    @Override
    public DMXObject getObject(long id) {
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
    public void fireEvent(DMXEvent event, Object... params) {
        em.fireEvent(event, params);
    }

    @Override
    public void dispatchEvent(String pluginUri, DMXEvent event, Object... params) {
        em.dispatchEvent(getPlugin(pluginUri), event, params);
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
    public WebSocketsServiceImpl getWebSocketsService() {
        return wsService;
    }

    @Override
    public Object getDatabaseVendorObject() {
        return pl.getDatabaseVendorObject();
    }

    // ---

    // Note: not part of public interface
    // Called from CoreActivator
    public void shutdown() {
        wsService.shutdown();
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
            // Create topic type "Data Type"
            // ### Note: the topic type "Data Type" depends on the data type "Text" and the data type "Text" in turn
            // depends on the topic type "Data Type". To resolve this circle we use a low-level (storage) call here
            // and postpone the data type association.
            TopicModel dataType = mf.newTopicTypeModel("dm4.core.data_type", "Data Type", "dm4.core.text");
            _createTopic(dataType);
            // Create data type "Text"
            TopicModel text = mf.newTopicModel("dm4.core.text", "dm4.core.data_type", new SimpleValue("Text"));
            _createTopic(text);
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
            pl.createTopicInstantiation(text.getId(), text.getTypeUri());
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
            // in DMXObjectImpl.store().)
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
