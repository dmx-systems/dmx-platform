package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
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
import systems.dmx.core.service.QueryResult;
import systems.dmx.core.service.TopicResult;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.storage.spi.DMXTransaction;

import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



/**
 * Implementation of the DMX core service. Embeddable into Java applications.
 */
public class CoreServiceImpl implements CoreService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    BundleContext bundleContext;
    AccessLayer al;
    EventManager em;
    ModelFactoryImpl mf;
    PrivilegedAccess ac;
    MigrationManager migrationManager;
    PluginManager pluginManager;
    WebSocketServiceImpl wss;
    WebPublishingService wpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DMX Core bundle.
     */
    public CoreServiceImpl(AccessLayer al, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.al = al;
        this.em = al.em;
        this.mf = al.mf;
        this.ac = new PrivilegedAccessImpl(al);
        this.migrationManager = new MigrationManager(this);
        this.pluginManager = new PluginManager(this);
        this.wss = new WebSocketServiceImpl(this);
        this.wpService = new WebPublishingService(al, wss);
        //
        setupDB();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *******************
    // *** CoreService ***
    // *******************



    // === Topics ===

    @Override
    public Topic getTopic(long topicId) {
        return al.getTopic(topicId).instantiate();
    }

    @Override
    public Topic getTopicByUri(String uri) {
        TopicModelImpl topic = al.getTopicByUri(uri);
        return topic != null ? topic.instantiate() : null;
    }

    @Override
    public List<Topic> getTopicsByType(String topicTypeUri) {
        return al.instantiate(al.getTopicsByType(topicTypeUri));
    }

    @Override
    public Iterable<Topic> getAllTopics() {
        return new InstantiationIterable(al.getAllTopics());
    }

    // ---

    @Override
    public Topic getTopicByValue(String typeUri, SimpleValue value) {
        TopicModelImpl topic = al.getTopicByValue(typeUri, value);
        return topic != null ? topic.instantiate() : null;
    }

    @Override
    public List<Topic> getTopicsByValue(String typeUri, SimpleValue value) {
        return al.instantiate(al.getTopicsByValue(typeUri, value));
    }

    @Override
    public List<Topic> queryTopics(String typeUri, String query) {
        return al.instantiate(al.queryTopics(typeUri, query));
    }

    @Override
    public TopicResult queryTopicsFulltext(String query, String typeUri, boolean searchChildTopics) {
        return new TopicResult(
            query, typeUri, searchChildTopics,
            al.instantiate(al.queryTopicsFulltext(query, typeUri, searchChildTopics))
        );
    }

    // ---

    @Override
    public TopicImpl createTopic(TopicModel model) {
        return al.createTopic((TopicModelImpl) model).instantiate();
    }

    @Override
    public void updateTopic(TopicModel updateModel) {
        al.updateTopic((TopicModelImpl) updateModel);
    }

    @Override
    public void deleteTopic(long topicId) {
        al.deleteTopic(topicId);
    }



    // === Associations ===

    @Override
    public Assoc getAssoc(long assocId) {
        return al.getAssoc(assocId).instantiate();
    }

    @Override
    public List<PlayerModel> getPlayerModels(long assocId) {
        return al.getPlayerModels(assocId);
    }

    @Override
    public List<Assoc> getAssocsByType(String assocTypeUri) {
        return al.instantiate(al.getAssocsByType(assocTypeUri));
    }

    @Override
    public List<Assoc> getAssocs(long topic1Id, long topic2Id) {
        return al.instantiate(al.getAssocs(topic1Id, topic2Id));
    }

    @Override
    public List<Assoc> getAssocs(long topic1Id, long topic2Id, String assocTypeUri) {
        return al.instantiate(al.getAssocs(assocTypeUri, topic1Id, topic2Id));
    }

    @Override
    public Assoc getAssocBetweenTopicAndTopic(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                              String roleTypeUri2) {
        AssocModelImpl assoc = al.getAssocBetweenTopicAndTopic(assocTypeUri, topic1Id, topic2Id, roleTypeUri1,
            roleTypeUri2);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public Assoc getAssocBetweenTopicAndAssoc(String assocTypeUri, long topicId, long assocId, String topicRoleTypeUri,
                                              String assocRoleTypeUri) {
        AssocModelImpl assoc = al.getAssocBetweenTopicAndAssoc(assocTypeUri, topicId, assocId, topicRoleTypeUri,
            assocRoleTypeUri);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public Iterable<Assoc> getAllAssocs() {
        return new InstantiationIterable(al.getAllAssocs());
    }

    // ---

    @Override
    public Assoc getAssocByValue(String typeUri, SimpleValue value) {
        AssocModelImpl assoc = al.getAssocByValue(typeUri, value);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public List<Assoc> queryAssocs(String typeUri, String query) {
        return al.instantiate(al.queryAssocs(typeUri, query));
    }

    // ---

    @Override
    public AssocImpl createAssoc(AssocModel model) {
        return al.createAssoc((AssocModelImpl) model).instantiate();
    }

    @Override
    public void updateAssoc(AssocModel updateModel) {
        al.updateAssoc((AssocModelImpl) updateModel);
    }

    @Override
    public void deleteAssoc(long assocId) {
        al.deleteAssoc(assocId);
    }



    // === Topic Types ===

    @Override
    public TopicTypeImpl getTopicType(String uri) {
        return al.getTopicType(uri).instantiate();
    }

    @Override
    public TopicTypeImpl getTopicTypeImplicitly(long topicId) {
        return al.getTopicTypeImplicitly(topicId).instantiate();
    }

    @Override
    public List<TopicType> getAllTopicTypes() {
        return al.instantiate(al.getAllTopicTypes());
    }

    // ---

    @Override
    public TopicTypeImpl createTopicType(TopicTypeModel model) {
        return al.createTopicType((TopicTypeModelImpl) model).instantiate();
    }

    @Override
    public void updateTopicType(TopicTypeModel updateModel) {
        al.updateTopicType((TopicTypeModelImpl) updateModel);
    }

    @Override
    public void deleteTopicType(String topicTypeUri) {
        al.deleteTopicType(topicTypeUri);
    }



    // === Assoc Types ===

    @Override
    public AssocTypeImpl getAssocType(String uri) {
        return al.getAssocType(uri).instantiate();
    }

    @Override
    public AssocTypeImpl getAssocTypeImplicitly(long assocId) {
        return al.getAssocTypeImplicitly(assocId).instantiate();
    }

    @Override
    public List<AssocType> getAllAssocTypes() {
        return al.instantiate(al.getAllAssocTypes());
    }

    // ---

    @Override
    public AssocTypeImpl createAssocType(AssocTypeModel model) {
        return al.createAssocType((AssocTypeModelImpl) model).instantiate();
    }

    @Override
    public void updateAssocType(AssocTypeModel updateModel) {
        al.updateAssocType((AssocTypeModelImpl) updateModel);
    }

    @Override
    public void deleteAssocType(String assocTypeUri) {
        al.deleteAssocType(assocTypeUri);
    }



    // === Role Types ===

    @Override
    public Topic createRoleType(TopicModel model) {
        return al.createRoleType((TopicModelImpl) model).instantiate();
    }



    // === Generic Object ===

    @Override
    public DMXObject getObject(long id) {
        return al.getObject(id).instantiate();
    }

    @Override
    public QueryResult query(String topicQuery, String topicTypeUri, boolean searchTopicChildren,
                             String assocQuery, String assocTypeUri, boolean searchAssocChildren) {
        return new QueryResult(
            topicQuery, topicTypeUri, searchTopicChildren,
            assocQuery, assocTypeUri, searchAssocChildren,
            al.instantiate(al.query(
                topicQuery, topicTypeUri, searchTopicChildren,
                assocQuery, assocTypeUri, searchAssocChildren
            ))
        );
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
        return al.db.fetchProperty(id, propUri);
    }

    @Override
    public boolean hasProperty(long id, String propUri) {
        return al.db.hasProperty(id, propUri);
    }

    // ---

    @Override
    public List<Topic> getTopicsByProperty(String propUri, Object propValue) {
        return al.instantiate(al.getTopicsByProperty(propUri, propValue));
    }

    @Override
    public List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return al.instantiate(al.getTopicsByPropertyRange(propUri, from, to));
    }

    @Override
    public List<Assoc> getAssocsByProperty(String propUri, Object propValue) {
        return al.instantiate(al.getAssocsByProperty(propUri, propValue));
    }

    @Override
    public List<Assoc> getAssocsByPropertyRange(String propUri, Number from, Number to) {
        return al.instantiate(al.getAssocsByPropertyRange(propUri, from, to));
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
                al.db.indexTopicProperty(topic.getId(), propUri, value);
                added++;
            }
            topics++;
        }
        logger.info("########## Adding topic property index complete\n    Topics processed: " + topics +
            "\n    added to index: " + added);
    }

    @Override
    public void addAssocPropertyIndex(String propUri) {
        int assocs = 0;
        int added = 0;
        logger.info("########## Adding association property index for \"" + propUri + "\"");
        for (Assoc assoc : getAllAssocs()) {
            if (assoc.hasProperty(propUri)) {
                Object value = assoc.getProperty(propUri);
                al.db.indexAssocProperty(assoc.getId(), propUri, value);
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
        return al.db.beginTx();
    }

    // ---

    @Override
    public ModelFactory getModelFactory() {
        return mf;
    }

    @Override
    public PrivilegedAccess getPrivilegedAccess() {
        return ac;
    }

    @Override
    public WebSocketServiceImpl getWebSocketService() {
        return wss;
    }

    @Override
    public Object getDatabaseVendorObject() {
        return al.db.getDatabaseVendorObject();
    }

    // ---

    // Note: not part of public interface
    // Called from CoreActivator
    public void shutdown() {
        wss.stop();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Helper ===

    /**
     * Convenience method.
     */
    Assoc createAssoc(String typeUri, PlayerModel player1, PlayerModel player2) {
        return createAssoc(mf.newAssocModel(typeUri, player1, player2));
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
            boolean isCleanInstall = al.sd.init();
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
            al.db.shutdown();
            throw new RuntimeException("Setting up the database failed", e);
        }
    }

    private void setupBootstrapContent() {
        try {
            // Create meta types "Topic Type" and "Association Type" -- needed to create topic types and assoc types
            TopicModelImpl t = mf.newTopicModel(TOPIC_TYPE, META_TYPE, new SimpleValue("Topic Type"));
            TopicModelImpl a = mf.newTopicModel(ASSOC_TYPE, META_TYPE, new SimpleValue("Association Type"));
            _createTopic(t);
            _createTopic(a);
            // Create topic type "Data Type"
            // ### Note: the topic type "Data Type" depends on the data type "Text" and the data type "Text" in turn
            // depends on the topic type "Data Type". To resolve this circle we use a low-level (storage) call here
            // and postpone the data type association.
            TopicModelImpl dataType = mf.newTopicTypeModel(DATA_TYPE, "Data Type", TEXT);
            _createTopic(dataType);
            // Create data type "Text"
            TopicModelImpl text = mf.newTopicModel(TEXT, DATA_TYPE, new SimpleValue("Text"));
            _createTopic(text);
            // Create association type "Composition" -- needed to associate topic/association types with data types
            TopicModelImpl composition = mf.newAssocTypeModel(COMPOSITION, "Composition", TEXT);
            _createTopic(composition);
            // Create association type "Instantiation" -- needed to associate topics with topic types
            TopicModelImpl instn = mf.newAssocTypeModel(INSTANTIATION, "Instantiation", TEXT);
            _createTopic(instn);
            //
            // 1) Postponed topic type association
            //
            // Note: createTopicInstantiation() creates the associations by *low-level* (storage) calls.
            // That's why the associations can be created *before* their type (here: "dmx.core.instantiation")
            // is fully constructed (the type's data type is not yet associated => step 2).
            al.createTopicInstantiation(t.getId(), t.getTypeUri());
            al.createTopicInstantiation(a.getId(), a.getTypeUri());
            al.createTopicInstantiation(dataType.getId(), dataType.getTypeUri());
            al.createTopicInstantiation(text.getId(), text.getTypeUri());
            al.createTopicInstantiation(composition.getId(), composition.getTypeUri());
            al.createTopicInstantiation(instn.getId(), instn.getTypeUri());
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
            // (see AssocImpl.store(): storage.storeAssoc() is called before getType() in DMXObjectImpl.store().)
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
            _associateDataType(META_TYPE,  TEXT);
            _associateDataType(TOPIC_TYPE, TEXT);
            _associateDataType(ASSOC_TYPE, TEXT);
            _associateDataType(DATA_TYPE,  TEXT);
            //
            _associateDataType(COMPOSITION,   TEXT);
            _associateDataType(INSTANTIATION, TEXT);
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
        al.db.storeTopic(model);
        al.db.storeTopicValue(model.id, model.value, model.typeUri, false);         // isHtml=false
    }

    /**
     * Low-level method that stores an (data type) association without its "Instantiation" association.
     * Needed for bootstrapping.
     */
    private void _associateDataType(String typeUri, String dataTypeUri) {
        AssocModelImpl assoc = mf.newAssocModel(COMPOSITION,
            mf.newTopicPlayerModel(typeUri,     PARENT),
            mf.newTopicPlayerModel(dataTypeUri, CHILD)
        );
        al.db.storeAssoc(assoc);
        al.db.storeAssocValue(assoc.id, assoc.value, assoc.typeUri, false);         // isHtml=false
    }
}
