package de.deepamehta.datomic;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import datomic.Connection;
import datomic.Peer;
import static datomic.Util.map;
import static datomic.Util.list;
import static datomic.Util.read;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;



public class DatomicStorage implements DeepaMehtaStorage {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String TEMP_ID = "temp-id";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private boolean isCleanInstall;

    private Connection conn = null;

    private ModelFactory mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    DatomicStorage(String databaseUri, ModelFactory mf) {
        ClassLoader savedCL = null;
        try {
            savedCL = setClassLoader();
            //
            this.isCleanInstall = Peer.createDatabase(databaseUri);
            if (!isCleanInstall) {  // ### FIXME: drop this
                throw new RuntimeException("Database already exists");
            }
            this.conn = Peer.connect(databaseUri);
            this.mf = mf;
        } catch (Throwable e) {
            if (conn != null) {
                shutdown();
            }
            throw new RuntimeException("Creating the Datomic instance failed (databaseUri=\"" + databaseUri + "\")", e);
        } finally {
            Thread.currentThread().setContextClassLoader(savedCL);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaStorage Implementation ***
    // ****************************************



    // === Topics ===

    @Override
    public TopicModel fetchTopic(long topicId) {
        return null;
    }

    @Override
    public TopicModel fetchTopic(String key, Object value) {
        return null;
    }

    @Override
    public List<TopicModel> fetchTopics(String key, Object value) {
        return null;
    }

    @Override
    public List<TopicModel> queryTopics(Object value) {
        return null;
    }

    @Override
    public List<TopicModel> queryTopics(String key, Object value) {
        return null;
    }

    @Override
    public Iterator<TopicModel> fetchAllTopics() {
        return null;
    }

    // ---

    @Override
    public void storeTopic(TopicModel topicModel) {
        setDefaults(topicModel);
        String uri = topicModel.getUri();
        checkUriUniqueness(uri);
        //
        // 1) update DB
        long topicId = resolveTempId(storeEntity(
            ":db/id", TEMP_ID,
            ":dm4/object-type", ":dm4.object-type/topic",
            ":dm4.object/uri", uri,
            ":dm4.object/type", topicModel.getTypeUri()));
        //
        // 2) update model
        topicModel.setId(topicId);
    }

    @Override
    public void storeTopicUri(long topicId, String uri) {
        checkUriUniqueness(uri);
        storeEntity(
            ":db/id", topicId,
            ":dm4.object/uri", uri);
    }

    // Note: a storage implementation is not responsible for maintaining the "Instantiation" associations.
    // This is performed at the application layer.
    @Override
    public void storeTopicTypeUri(long topicId, String topicTypeUri) {
        storeEntity(
            ":db/id", topicId,
            ":dm4.object/type", topicTypeUri);
    }

    @Override
    public void storeTopicValue(long topicId, SimpleValue value, List<IndexMode> indexModes,
                                                                 String indexKey, SimpleValue indexValue) {
    }

    @Override
    public void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue indexValue) {
    }

    // ---

    @Override
    public void deleteTopic(long topicId) {
    }



    // === Associations ===

    @Override
    public AssociationModel fetchAssociation(long assocId) {
        return null;
    }

    @Override
    public AssociationModel fetchAssociation(String key, Object value) {
        return null;
    }

    @Override
    public List<AssociationModel> fetchAssociations(String key, Object value) {
        return null;
    }

    @Override
    public List<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2,
                                                                         String roleTypeUri1, String roleTypeUri2) {
        return null;
    }

    @Override
    public List<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        return null;
    }

    @Override
    public Iterator<AssociationModel> fetchAllAssociations() {
        return null;
    }

    @Override
    public long[] fetchPlayerIds(long assocId) {
        return null;
    }

    // ---

    @Override
    public void storeAssociation(AssociationModel assocModel) {
    }

    @Override
    public void storeAssociationUri(long assocId, String uri) {
    }

    // Note: a storage implementation is not responsible for maintaining the "Instantiation" associations.
    // This is performed at the application layer.
    @Override
    public void storeAssociationTypeUri(long assocId, String assocTypeUri) {
    }

    @Override
    public void storeAssociationValue(long assocId, SimpleValue value, List<IndexMode> indexModes,
                                                                       String indexKey, SimpleValue indexValue) {
    }

    @Override
    public void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue indexValue) {
    }

    @Override
    public void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
    }

    // ---

    @Override
    public void deleteAssociation(long assocId) {
    }



    // === Generic Object ===

    @Override
    public DeepaMehtaObjectModel fetchObject(long id) {
        return null;
    }



    // === Traversal ===

    @Override
    public List<AssociationModel> fetchTopicAssociations(long topicId) {
        return null;
    }

    @Override
    public List<AssociationModel> fetchAssociationAssociations(long assocId) {
        return null;
    }

    // ---

    @Override
    public List<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersTopicTypeUri) {
        return null;
    }

    @Override
    public List<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return null;
    }

    // ---

    @Override
    public List<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        return null;
    }

    @Override
    public List<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return null;
    }

    // ---

    @Override
    public List<RelatedTopicModel> fetchRelatedTopics(long id, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return null;
    }

    @Override
    public List<RelatedAssociationModel> fetchRelatedAssociations(long id, String assocTypeUri, String myRoleTypeUri,
                                                                  String othersRoleTypeUri, String othersAssocTypeUri) {
        return null;
    }



    // === Properties ===

    @Override
    public Object fetchProperty(long id, String propUri) {
        return null;
    }

    @Override
    public boolean hasProperty(long id, String propUri) {
        return false;
    }

    // ---

    @Override
    public List<TopicModel> fetchTopicsByProperty(String propUri, Object propValue) {
        return null;
    }

    @Override
    public List<TopicModel> fetchTopicsByPropertyRange(String propUri, Number from, Number to) {
        return null;
    }

    @Override
    public List<AssociationModel> fetchAssociationsByProperty(String propUri, Object propValue) {
        return null;
    }

    @Override
    public List<AssociationModel> fetchAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return null;
    }

    // ---

    @Override
    public void storeTopicProperty(long topicId, String propUri, Object propValue, boolean addToIndex) {
    }

    @Override
    public void storeAssociationProperty(long assocId, String propUri, Object propValue, boolean addToIndex) {
    }

    // ---

    @Override
    public void indexTopicProperty(long topicId, String propUri, Object propValue) {
    }

    @Override
    public void indexAssociationProperty(long assocId, String propUri, Object propValue) {
    }

    // ---

    @Override
    public void deleteTopicProperty(long topicId, String propUri) {
    }

    @Override
    public void deleteAssociationProperty(long assocId, String propUri) {
    }



    // === DB ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return new DatomicTransactionAdapter();
    }

    @Override
    public boolean init() {
        try {
            if (isCleanInstall) {
                installSchema();
                // create root topic ### TODO
                storeEntity(":dm4/object-type", ":dm4.object-type/topic");
            }
            return isCleanInstall;
        } catch (Exception e) {
            throw new RuntimeException("Initializing the Datomic instance failed", e);
        }
    }

    @Override
    public void shutdown() {
        Peer.shutdown(true);
    }

    // ---

    @Override
    public Object getDatabaseVendorObject() {
        return null;
    }

    @Override
    public Object getDatabaseVendorObject(long objectId) {
        return null;
    }

    // ---

    @Override
    public ModelFactory getModelFactory() {
        return mf;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void installSchema() {
        transact(
            // DM4 Object-Type
            map(":db/ident",       ":dm4/object-type",
                ":db/valueType",   ":db.type/ref",
                ":db/cardinality", ":db.cardinality/one",
                ":db/doc",         "A DM4 object type (topic or assoc)"),
            map(":db/ident", ":dm4.object-type/topic"),
            map(":db/ident", ":dm4.object-type/assoc"),
            // DM4 Object
            map(":db/ident",       ":dm4.object/uri",
                ":db/valueType",   ":db.type/string",
                ":db/cardinality", ":db.cardinality/one",
                ":db/doc",         "A DM4 object's URI"),
            map(":db/ident",       ":dm4.object/type",
                ":db/valueType",   ":db.type/string",
                ":db/cardinality", ":db.cardinality/one",
                ":db/doc",         "A DM4 object's type (URI)"));
    }

    // ---

    Collection query(String query, Object... inputs) {
        return Peer.query(query, conn.db(), inputs);
    }

    Collection query(List find, List in, List where, Object... inputs) {
        return Peer.query(map(
            read(":find"),  find,
            read(":in"),    in,
            read(":where"), where), conn.db(), inputs);
    }

    Future<Map> storeEntity(Object... keyvals) {
        return transact(map(keyvals));
    }

    Future<Map> transact(Map... maps) {
        return conn.transact(list(maps));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: a principal copy exists in DeepaMehtaObjectModel
    private void setDefaults(DeepaMehtaObjectModel model) {
        if (model.getUri() == null) {
            model.setUri("");
        }
        if (model.getSimpleValue() == null) {
            model.setSimpleValue("");
        }
    }

    /**
     * Checks if a topic or an association with the given URI exists in the DB, and
     * throws an exception if so. If an empty URI ("") is given no check is performed.
     *
     * @param   uri     The URI to check. Must not be null.
     */
    private void checkUriUniqueness(String uri) {
        if (!uri.equals("")) {
            Collection result = query(list(read("?e")), list(read("$")),
                list(list(read("?e"), ":dm4.object/uri", uri)));
            if (result.size() != 0) {
                throw new RuntimeException("URI \"" + uri + "\" is not unique");
            }
        }
    }

    private long resolveTempId(Future<Map> txInfo) {
        try {
            return (Long) Peer.resolveTempid(conn.db(), txInfo.get().get(Connection.TEMPIDS), TEMP_ID);
        } catch (Exception e) {
            throw new RuntimeException("Resolving a temporary ID failed", e);
        }
    }

    // ---

    private ClassLoader setClassLoader() {
        Thread t = Thread.currentThread();
        ClassLoader savedCL = t.getContextClassLoader();
        t.setContextClassLoader(DatomicStorage.class.getClassLoader());
        return savedCL;
    }
}
