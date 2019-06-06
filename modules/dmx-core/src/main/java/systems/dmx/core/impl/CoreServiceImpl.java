package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.service.PluginInfo;
import systems.dmx.core.service.accesscontrol.AccessControl;
import systems.dmx.core.storage.spi.DMXTransaction;

import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.logging.Logger;



/**
 * Implementation of the DMX core service. Embeddable into Java applications.
 */
public class CoreServiceImpl implements CoreService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    BundleContext bundleContext;
    PersistenceLayer pl;
    EventManager em;
    ModelFactoryImpl mf;
    MigrationManager migrationManager;
    PluginManager pluginManager;
    AccessControl accessControl;
    WebSocketsServiceImpl wsService;
    WebPublishingService wpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DMX Core bundle.
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
    public Assoc getAssoc(long assocId) {
        return pl.getAssoc(assocId);
    }

    @Override
    public Assoc getAssocByValue(String key, SimpleValue value) {
        return pl.getAssocByValue(key, value);
    }

    @Override
    public List<Assoc> getAssocsByValue(String key, SimpleValue value) {
        return pl.getAssocsByValue(key, value);
    }

    @Override
    public Assoc getAssoc(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1, String roleTypeUri2) {
        return pl.getAssoc(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
    }

    @Override
    public Assoc getAssocBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                    String topicRoleTypeUri, String assocRoleTypeUri) {
        return pl.getAssocBetweenTopicAndAssociation(assocTypeUri, topicId, assocId, topicRoleTypeUri,
            assocRoleTypeUri);
    }

    // ---

    @Override
    public List<Assoc> getAssocsByType(String assocTypeUri) {
        return pl.getAssocsByType(assocTypeUri);
    }

    @Override
    public List<Assoc> getAssocs(long topic1Id, long topic2Id) {
        return pl.getAssocs(topic1Id, topic2Id);
    }

    @Override
    public List<Assoc> getAssocs(long topic1Id, long topic2Id, String assocTypeUri) {
        return pl.getAssocs(assocTypeUri, topic1Id, topic2Id);
    }

    // ---

    @Override
    public Iterable<Assoc> getAllAssociations() {
        return pl.getAllAssociations();
    }

    @Override
    public List<PlayerModel> getRoleModels(long assocId) {
        return pl.getRoleModels(assocId);
    }

    // ---

    @Override
    public AssocImpl createAssoc(AssocModel model) {
        return pl.createAssoc((AssocModelImpl) model);
    }

    @Override
    public void updateAssociation(AssocModel updateModel) {
        pl.updateAssociation((AssocModelImpl) updateModel);
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



    // === Assoc Types ===

    @Override
    public AssocTypeImpl getAssocType(String uri) {
        return pl.getAssocType(uri);
    }

    @Override
    public AssocTypeImpl getAssocTypeImplicitly(long assocId) {
        return pl.getAssocTypeImplicitly(assocId);
    }

    // ---

    @Override
    public List<AssocType> getAllAssocTypes() {
        return pl.getAllAssocTypes();
    }

    // ---

    @Override
    public AssocTypeImpl createAssocType(AssocTypeModel model) {
        return pl.createAssocType((AssocTypeModelImpl) model);
    }

    @Override
    public void updateAssocType(AssocTypeModel updateModel) {
        pl.updateAssocType((AssocTypeModelImpl) updateModel);
    }

    @Override
    public void deleteAssocType(String assocTypeUri) {
        pl.deleteAssocType(assocTypeUri);
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
    public List<Assoc> getAssocsByProperty(String propUri, Object propValue) {
        return pl.getAssocsByProperty(propUri, propValue);
    }

    @Override
    public List<Assoc> getAssocsByPropertyRange(String propUri, Number from, Number to) {
        return pl.getAssocsByPropertyRange(propUri, from, to);
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
        for (Assoc assoc : getAllAssociations()) {
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
    public DMXTransaction beginTx() {
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
    Assoc createAssoc(String typeUri, PlayerModel roleModel1, PlayerModel roleModel2) {
        return createAssoc(mf.newAssocModel(typeUri, roleModel1, roleModel2));
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
        DMXTransaction tx = beginTx();
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
            // Create meta types "Topic Type" and "Association Type" -- needed to create topic types and assoc types
            TopicModelImpl t = mf.newTopicModel("dmx.core.topic_type", "dmx.core.meta_type",
                new SimpleValue("Topic Type"));
            TopicModelImpl a = mf.newTopicModel("dmx.core.assoc_type", "dmx.core.meta_type",
                new SimpleValue("Association Type"));
            _createTopic(t);
            _createTopic(a);
            // Create topic type "Data Type"
            // ### Note: the topic type "Data Type" depends on the data type "Text" and the data type "Text" in turn
            // depends on the topic type "Data Type". To resolve this circle we use a low-level (storage) call here
            // and postpone the data type association.
            TopicModelImpl dataType = mf.newTopicTypeModel("dmx.core.data_type", "Data Type", "dmx.core.text");
            _createTopic(dataType);
            // Create data type "Text"
            TopicModelImpl text = mf.newTopicModel("dmx.core.text", "dmx.core.data_type", new SimpleValue("Text"));
            _createTopic(text);
            // Create association type "Composition" -- needed to associate topic/association types with data types
            TopicModelImpl composition = mf.newAssocTypeModel("dmx.core.composition", "Composition", "dmx.core.text");
            _createTopic(composition);
            // Create association type "Instantiation" -- needed to associate topics with topic types
            TopicModelImpl instn = mf.newAssocTypeModel("dmx.core.instantiation", "Instantiation", "dmx.core.text");
            _createTopic(instn);
            //
            // 1) Postponed topic type association
            //
            // Note: createTopicInstantiation() creates the associations by *low-level* (storage) calls.
            // That's why the associations can be created *before* their type (here: "dmx.core.instantiation")
            // is fully constructed (the type's data type is not yet associated => step 2).
            pl.createTopicInstantiation(t.getId(), t.getTypeUri());
            pl.createTopicInstantiation(a.getId(), a.getTypeUri());
            pl.createTopicInstantiation(dataType.getId(), dataType.getTypeUri());
            pl.createTopicInstantiation(text.getId(), text.getTypeUri());
            pl.createTopicInstantiation(composition.getId(), composition.getTypeUri());
            pl.createTopicInstantiation(instn.getId(), instn.getTypeUri());
            //
            // 2) Postponed data type association
            //
            // Note: associateDataType() creates the association by a *high-level* (service) call.
            // This requires the association type (here: dmx.core.composition) to be fully constructed already.
            // That's why the topic type associations (step 1) must be performed *before* the data type associations.
            // ### FIXDOC: not true anymore
            //
            // Note: at time of the first associateDataType() call the required association type (dmx.core.composition)
            // is *not* fully constructed yet! (it gets constructed through this very call). This works anyway because
            // the data type assigning association is created *before* the association type is fetched.
            // (see AssocImpl.store(): storage.storeAssociation() is called before getType() in DMXObjectImpl.store().)
            // ### FIXDOC: not true anymore
            //
            // Important is that associateDataType("dmx.core.composition") is the first call here.
            // ### FIXDOC: not true anymore
            //
            // Note: _associateDataType() creates the data type assigning association by a *low-level* (storage) call.
            // A high-level (service) call would fail while setting the association's value. The involved getType()
            // would fail (not because the association is missed -- it's created meanwhile, but)
            // because this involves fetching the association including its value. The value doesn't exist yet,
            // because its setting forms the begin of this vicious circle.
            _associateDataType("dmx.core.meta_type",  "dmx.core.text");
            _associateDataType("dmx.core.topic_type", "dmx.core.text");
            _associateDataType("dmx.core.assoc_type", "dmx.core.text");
            _associateDataType("dmx.core.data_type",  "dmx.core.text");
            //
            _associateDataType("dmx.core.composition",   "dmx.core.text");
            _associateDataType("dmx.core.instantiation", "dmx.core.text");
        } catch (Exception e) {
            throw new RuntimeException("Setting up the bootstrap content failed", e);
        }
    }

    // ---

    /**
     * Low-level method that stores a topic without its "Instantiation" association.
     * Needed for bootstrapping.
     */
    private void _createTopic(TopicModelImpl model) {
        pl.storeTopic(model);
        pl.storeTopicValue(model.id, model.value, model.typeUri, false);            // isHtml=false
    }

    /**
     * Low-level method that stores an (data type) association without its "Instantiation" association.
     * Needed for bootstrapping.
     */
    private void _associateDataType(String typeUri, String dataTypeUri) {
        AssocModelImpl assoc = mf.newAssocModel("dmx.core.composition",
            mf.newTopicRoleModel(typeUri,     "dmx.core.type"),
            mf.newTopicRoleModel(dataTypeUri, "dmx.core.default")
        );
        pl.storeAssociation(assoc);
        pl.storeAssociationValue(assoc.id, assoc.value, assoc.typeUri, false);      // isHtml=false
    }
}
