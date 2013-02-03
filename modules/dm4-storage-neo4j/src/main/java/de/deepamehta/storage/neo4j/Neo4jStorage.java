package de.deepamehta.storage.neo4j;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class Neo4jStorage implements DeepaMehtaStorage {

    // ------------------------------------------------------------------------------------------------------- Constants

    // --- DB Property Keys ---
    private static final String KEY_NODE_TYPE = "node_type";
    private static final String KEY_VALUE     = "value";

    // --- Content Indexes ---
    private static final String KEY_URI      = "uri";                       // used as property key as well
    private static final String KEY_TPYE_URI = "type_uri";                  // used as property key as well
    private static final String KEY_FULLTEXT = "_fulltext_";

    // --- Association Metadata Index ---
    private static final String KEY_ASSOC_ID       = "assoc_id";
    private static final String KEY_ASSOC_TPYE_URI = "assoc_type_uri";
    // role 1 & 2
    private static final String KEY_ROLE_TPYE_URI   = "role_type_uri_";     // "1" or "2" is appended programatically
    private static final String KEY_PLAYER_TPYE     = "player_type_";       // "1" or "2" is appended programatically
    private static final String KEY_PLAYER_ID       = "player_id_";         // "1" or "2" is appended programatically
    private static final String KEY_PLAYER_TYPE_URI = "player_type_uri_";   // "1" or "2" is appended programatically

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private GraphDatabaseService neo4j = null;
    private RelationtypeCache relTypeCache;
    
    private Index<Node> topicContentExact;
    private Index<Node> topicContentFulltext;
    private Index<Node> assocContentExact;
    private Index<Node> assocContentFulltext;
    private Index<Node> assocMetadata;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    Neo4jStorage(String databasePath) {
        try {
            this.neo4j = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);
            this.relTypeCache = new RelationtypeCache(neo4j);
            // indexes
            this.topicContentExact    = createExactIndex("topic-content-exact");
            this.topicContentFulltext = createFulltextIndex("topic-content-fulltext");
            this.assocContentExact    = createExactIndex("assoc-content-exact");
            this.assocContentFulltext = createFulltextIndex("assoc-content-fulltext");
            this.assocMetadata = createExactIndex("assoc-metadata");
        } catch (Exception e) {
            if (neo4j != null) {
                shutdown();
            }
            throw new RuntimeException("Creating the Neo4j instance and indexes failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** DeepaMehtaStorage Implementation ***
    // ****************************************



    // === Topics ===

    @Override
    public TopicModel fetchTopic(long topicId) {
        return buildTopic(fetchTopicNode(topicId));
    }

    @Override
    public TopicModel fetchTopic(String key, Object value) {
        Node node = topicContentExact.get(key, value).getSingle();
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
    public List<TopicModel> fetchTopics(String key, Object value) {
        return buildTopics(topicContentExact.query(key, value));
    }

    // ---

    @Override
    public List<TopicModel> queryTopics(Object value) {
        return queryTopics(null, value);
    }

    @Override
    public List<TopicModel> queryTopics(String key, Object value) {
        if (key == null) {
            key = KEY_FULLTEXT;
        }
        if (value == null) {
            throw new IllegalArgumentException("Tried to call queryTopics() with a null value Object (key=\"" + key +
                "\")");
        }
        //
        return buildTopics(topicContentFulltext.query(key, value));
    }

    // ---

    @Override
    public void storeTopicUri(long topicId, String uri) {
        storeAndIndexTopicUri(fetchTopicNode(topicId), uri);
    }

    // Note: a storage implementation is not responsible for maintaining the "Instantiation" associations.
    // This is performed at the application layer.
    @Override
    public void storeTopicTypeUri(long topicId, String topicTypeUri) {
        Node topicNode = fetchTopicNode(topicId);
        //
        // 1) update DB and content index
        storeAndIndexTopicTypeUri(topicNode, topicTypeUri);
        //
        // 2) update association metadata index
        reindexTypeUri(topicNode, topicTypeUri);
    }

    @Override
    public void storeTopicValue(long topicId, SimpleValue value, Collection<IndexMode> indexModes,
                                                                 String indexKey, SimpleValue indexValue) {
        storeAndIndexTopicValue(fetchTopicNode(topicId), value.value(), indexModes, indexKey,
            getIndexValue(value, indexValue));
    }

    // ---

    @Override
    public void storeTopic(TopicModel topicModel) {
        String uri = topicModel.getUri();
        checkUriUniqueness(uri);
        //
        // 1) update DB
        Node topicNode = neo4j.createNode();
        topicNode.setProperty(KEY_NODE_TYPE, "topic");
        //
        storeAndIndexTopicUri(topicNode, uri);
        storeAndIndexTopicTypeUri(topicNode, topicModel.getTypeUri());
        //
        // 2) update model
        topicModel.setId(topicNode.getId());
    }

    @Override
    public void deleteTopic(long topicId) {
        // 1) update DB
        Node topicNode = fetchTopicNode(topicId);
        topicNode.delete();
        //
        // 2) update index
        removeTopicFromIndex(topicNode);
    }



    // === Associations ===

    @Override
    public AssociationModel fetchAssociation(long assocId) {
        return buildAssociation(fetchAssociationNode(assocId));
    }

    // ---

    @Override
    public Set<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2,
                                                                        String roleTypeUri1, String roleTypeUri2) {
        return queryAssociationIndex(
            assocTypeUri,
            roleTypeUri1, NodeType.TOPIC, topicId1, null,
            roleTypeUri2, NodeType.TOPIC, topicId2, null
        );
    }

    @Override
    public Set<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        return queryAssociationIndex(
            assocTypeUri,
            topicRoleTypeUri, NodeType.TOPIC, topicId, null,
            assocRoleTypeUri, NodeType.ASSOC, assocId, null
        );
    }

    // ---

    @Override
    public void storeAssociationUri(long assocId, String uri) {
        storeAndIndexAssociationUri(fetchAssociationNode(assocId), uri);
    }

    // Note: a storage implementation is not responsible for maintaining the "Instantiation" associations.
    // This is performed at the application layer.
    @Override
    public void storeAssociationTypeUri(long assocId, String assocTypeUri) {
        Node assocNode = fetchAssociationNode(assocId);
        //
        // 1) update DB and content index
        storeAndIndexAssociationTypeUri(assocNode, assocTypeUri);
        //
        // 2) update association metadata index
        indexAssociationType(assocNode, assocTypeUri);  // update association entry itself
        reindexTypeUri(assocNode, assocTypeUri);        // update all association entries the association is a player of
    }

    @Override
    public void storeAssociationValue(long assocId, SimpleValue value, Collection<IndexMode> indexModes,
                                                                       String indexKey, SimpleValue indexValue) {
        storeAndIndexAssociationValue(fetchAssociationNode(assocId), value.value(), indexModes, indexKey,
            getIndexValue(value, indexValue));
    }

    @Override
    public void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
        Node assocNode = fetchAssociationNode(assocId);
        //
        // 1) update DB
        fetchRelationship(assocNode, playerId).delete();                                        // delete relationship
        assocNode.createRelationshipTo(fetchNode(playerId), getRelationshipType(roleTypeUri));  // create new one
        //
        // 2) update association metadata index
        indexAssociationRoleType(assocNode, playerId, roleTypeUri);
    }

    // ---

    @Override
    public void storeAssociation(AssociationModel assocModel) {
        String uri = assocModel.getUri();
        checkUriUniqueness(uri);
        //
        // 1) update DB
        Node assocNode = neo4j.createNode();
        assocNode.setProperty(KEY_NODE_TYPE, "assoc");
        //
        storeAndIndexAssociationUri(assocNode, uri);
        storeAndIndexAssociationTypeUri(assocNode, assocModel.getTypeUri());
        //
        RoleModel role1 = assocModel.getRoleModel1();
        RoleModel role2 = assocModel.getRoleModel2();
        Node playerNode1 = storePlayerRelationship(assocNode, role1);
        Node playerNode2 = storePlayerRelationship(assocNode, role2);
        //
        // 2) update index
        indexAssociation(assocNode, role1.getRoleTypeUri(), playerNode1,
                                    role2.getRoleTypeUri(), playerNode2);
        // 3) update model
        assocModel.setId(assocNode.getId());
    }

    @Override
    public void deleteAssociation(long assocId) {
        // 1) update DB
        Node assocNode = fetchAssociationNode(assocId);
        // delete the 2 player relationships
        for (Relationship rel : fetchRelationships(assocNode)) {
            rel.delete();
        }
        //
        assocNode.delete();
        //
        // 2) update index
        removeAssociationFromIndex(assocNode);  // content index
        assocMetadata.remove(assocNode);        // metadata index
    }



    // === Traversal ===

    @Override
    public Set<AssociationModel> fetchTopicAssociations(long topicId) {
        return fetchAssociations(fetchTopicNode(topicId));
    }

    @Override
    public Set<AssociationModel> fetchAssociationAssociations(long assocId) {
        return fetchAssociations(fetchAssociationNode(assocId));
    }

    // ---

    @Override
    public Set<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersTopicTypeUri) {
        return buildRelatedTopics(queryAssociationIndex(
            assocTypeUri,
            myRoleTypeUri,     NodeType.TOPIC, topicId, null,
            othersRoleTypeUri, NodeType.TOPIC, -1,      othersTopicTypeUri
        ), topicId);
    }

    @Override
    public Set<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return buildRelatedAssociations(queryAssociationIndex(
            assocTypeUri,
            myRoleTypeUri,     NodeType.TOPIC, topicId, null,
            othersRoleTypeUri, NodeType.ASSOC, -1,      othersAssocTypeUri
        ), topicId);
    }

    // ---

    @Override
    public Set<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                String othersRoleTypeUri, String othersTopicTypeUri) {
        return buildRelatedTopics(queryAssociationIndex(
            assocTypeUri,
            myRoleTypeUri,     NodeType.ASSOC, assocId, null,
            othersRoleTypeUri, NodeType.TOPIC, -1,      othersTopicTypeUri
        ), assocId);
    }

    @Override
    public Set<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return buildRelatedAssociations(queryAssociationIndex(
            assocTypeUri,
            myRoleTypeUri,     NodeType.ASSOC, assocId, null,
            othersRoleTypeUri, NodeType.ASSOC, -1,      othersAssocTypeUri
        ), assocId);
    }

    // ---

    @Override
    public Set<RelatedTopicModel> fetchRelatedTopics(long id, String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersTopicTypeUri) {
        return buildRelatedTopics(queryAssociationIndex(
            assocTypeUri,
            myRoleTypeUri,     null,           id, null,
            othersRoleTypeUri, NodeType.TOPIC, -1, othersTopicTypeUri
        ), id);
    }

    @Override
    public Set<RelatedAssociationModel> fetchRelatedAssociations(long id, String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        return buildRelatedAssociations(queryAssociationIndex(
            assocTypeUri,
            myRoleTypeUri,     null,           id, null,
            othersRoleTypeUri, NodeType.ASSOC, -1, othersAssocTypeUri
        ), id);
    }



    // === Properties ===

    @Override
    public Object fetchProperty(long id, String key) {
        return fetchNode(id).getProperty(key);
    }

    @Override
    public void storeProperty(long id, String key, Object value) {
        fetchNode(id).setProperty(key, value);
    }

    @Override
    public boolean hasProperty(long id, String key) {
        return fetchNode(id).hasProperty(key);
    }



    // === DB ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return new Neo4jTransactionAdapter(neo4j);
    }

    @Override
    public boolean setupRootNode() {
        try {
            Node rootNode = fetchNode(0);
            //
            if (rootNode.getProperty(KEY_NODE_TYPE, null) != null) {
                return false;
            }
            //
            rootNode.setProperty(KEY_NODE_TYPE, "topic");
            rootNode.setProperty(KEY_VALUE, "Meta Type");
            storeAndIndexTopicUri(rootNode, "dm4.core.meta_type");
            storeAndIndexTopicTypeUri(rootNode, "dm4.core.meta_meta_type");
            //
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Setting up the root node (0) failed", e);
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutdown Neo4j");
        neo4j.shutdown();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Neo4j -> DeepaMehta Bridge ===

    private TopicModel buildTopic(Node node) {
        return new TopicModel(
            node.getId(),
            uri(node),
            typeUri(node),
            simpleValue(node),
            null    // composite=null
        );
    }

    private List<TopicModel> buildTopics(Iterable<Node> nodes) {
        List<TopicModel> topics = new ArrayList();
        for (Node node : nodes) {
            topics.add(buildTopic(node));
        }
        return topics;
    }

    // ---

    private AssociationModel buildAssociation(Node assocNode) {
        List<RoleModel> roleModels = buildRoleModels(assocNode);
        return new AssociationModel(
            assocNode.getId(),
            uri(assocNode),
            typeUri(assocNode),
            roleModels.get(0), roleModels.get(1),
            simpleValue(assocNode),
            null    // composite=null
        );
    }

    private List<RoleModel> buildRoleModels(Node assocNode) {
        List<RoleModel> roleModels = new ArrayList();
        for (Relationship rel : fetchRelationships(assocNode)) {
            Node node = rel.getEndNode();
            String roleTypeUri = rel.getType().name();
            RoleModel roleModel = NodeType.of(node).createRoleModel(node, roleTypeUri);
            roleModels.add(roleModel);
        }
        return roleModels;
    }



    // === DeepaMehta -> Neo4j Bridge ===

    private Node storePlayerRelationship(Node assocNode, RoleModel roleModel) {
        Node playerNode = fetchPlayerNode(roleModel);
        assocNode.createRelationshipTo(
            playerNode,
            getRelationshipType(roleModel.getRoleTypeUri())
        );
        return playerNode;
    }

    private Node fetchPlayerNode(RoleModel roleModel) {
        if (roleModel instanceof TopicRoleModel) {
            return fetchTopicPlayerNode((TopicRoleModel) roleModel);
        } else if (roleModel instanceof AssociationRoleModel) {
            return fetchAssociationNode(roleModel.getPlayerId());
        } else {
            throw new RuntimeException("Unexpected role model: " + roleModel);
        }
    }

    private Node fetchTopicPlayerNode(TopicRoleModel roleModel) {
        if (roleModel.topicIdentifiedByUri()) {
            return fetchTopicNodeByUri(roleModel.getTopicUri());
        } else {
            return fetchTopicNode(roleModel.getPlayerId());
        }
    }



    // === Neo4j Helper ===

    private Relationship fetchRelationship(Node assocNode, long playerId) {
        List<Relationship> rels = fetchRelationships(assocNode);
        boolean match1 = rels.get(0).getEndNode().getId() == playerId;
        boolean match2 = rels.get(1).getEndNode().getId() == playerId;
        if (match1 && match2) {
            throw new RuntimeException("Ambiguity: both players have ID " + playerId + " in association " +
                assocNode.getId());
        } else if (match1) {
            return rels.get(0);
        } else if (match2) {
            return rels.get(1);
        } else {
            throw new IllegalArgumentException("ID " + playerId + " is not a player in association " +
                assocNode.getId());
        }
    }

    private List<Relationship> fetchRelationships(Node assocNode) {
        List<Relationship> rels = new ArrayList();
        for (Relationship rel : assocNode.getRelationships(Direction.OUTGOING)) {
            rels.add(rel);
        }
        // sanity check
        if (rels.size() != 2) {
            throw new RuntimeException("Association " + assocNode.getId() + " connects " + rels.size() +
                " player instead of 2");
        }
        //
        return rels;
    }

    // ---

    private Set<AssociationModel> fetchAssociations(Node node) {
        Set<AssociationModel> assocs = new HashSet();
        for (Relationship rel : node.getRelationships(Direction.INCOMING)) {
            Node assocNode = rel.getStartNode();
            assocs.add(buildAssociation(assocNode));
        }
        return assocs;
    }

    // ---

    private Node fetchTopicNode(long topicId) {
        return checkType(
            fetchNode(topicId), NodeType.TOPIC
        );
    }

    private Node fetchAssociationNode(long assocId) {
        return checkType(
            fetchNode(assocId), NodeType.ASSOC
        );
    }

    // ---

    private Node fetchNode(long id) {
        return neo4j.getNodeById(id);
    }

    private Node fetchTopicNodeByUri(String uri) {
        Node node = topicContentExact.get(KEY_URI, uri).getSingle();
        //
        if (node == null) {
            throw new RuntimeException("Topic with URI \"" + uri + "\" not found in database");
        }
        //
        return checkType(node, NodeType.TOPIC);
    }

    private Node checkType(Node node, NodeType type) {
        if (NodeType.of(node) != type) {
            throw new IllegalArgumentException(type.error(node));
        }
        return node;
    }

    // ---

    private RelationshipType getRelationshipType(String typeName) {
        return relTypeCache.get(typeName);
    }

    // ---

    private String uri(Node node) {
        return (String) node.getProperty(KEY_URI);
    }

    private String typeUri(Node node) {
        return (String) node.getProperty(KEY_TPYE_URI);
    }

    private SimpleValue simpleValue(Node node) {
        return new SimpleValue(node.getProperty(KEY_VALUE));
    }



    // === DeepaMehta Helper ===

    // ### TODO: this is a DB agnostic helper method. It could be moved e.g. to a common base class.
    private Set<RelatedTopicModel> buildRelatedTopics(Collection<AssociationModel> assocs, long playerId) {
        Set<RelatedTopicModel> relTopics = new HashSet();
        for (AssociationModel assoc : assocs) {
            relTopics.add(new RelatedTopicModel(
                fetchTopic(
                    assoc.getOtherPlayerId(playerId)
                ), assoc)
            );
        }
        return relTopics;
    }

    // ### TODO: this is a DB agnostic helper method. It could be moved e.g. to a common base class.
    private Set<RelatedAssociationModel> buildRelatedAssociations(Collection<AssociationModel> assocs, long playerId) {
        Set<RelatedAssociationModel> relAssocs = new HashSet();
        for (AssociationModel assoc : assocs) {
            relAssocs.add(new RelatedAssociationModel(
                fetchAssociation(
                    assoc.getOtherPlayerId(playerId)
                ), assoc)
            );
        }
        return relAssocs;
    }

    // ---

    /**
     * Checks if a topic with the given URI exists in the database, and if so, throws an exception.
     * If an empty string is given no check is performed. ### FIXDOC
     *
     * @param   uri     The URI to check. Must not be null.
     */
    private void checkUriUniqueness(String uri) {
        if (uri.equals("")) {
            return;
        }
        Node n1 = topicContentExact.get(KEY_URI, uri).getSingle();
        Node n2 = assocContentExact.get(KEY_URI, uri).getSingle();
        if (n1 != null || n2 != null) {
            throw new RuntimeException("URI \"" + uri + "\" is not unique");
        }
    }



    // === Value Storage ===

    private void storeAndIndexTopicUri(Node topicNode, String uri) {
        storeAndIndexExactValue(topicNode, KEY_URI, uri, topicContentExact);
    }

    private void storeAndIndexAssociationUri(Node assocNode, String uri) {
        storeAndIndexExactValue(assocNode, KEY_URI, uri, assocContentExact);
    }

    // ---

    private void storeAndIndexTopicTypeUri(Node topicNode, String topicTypeUri) {
        storeAndIndexExactValue(topicNode, KEY_TPYE_URI, topicTypeUri, topicContentExact);
    }

    private void storeAndIndexAssociationTypeUri(Node assocNode, String assocTypeUri) {
        storeAndIndexExactValue(assocNode, KEY_TPYE_URI, assocTypeUri, assocContentExact);
    }

    // ---

    private void storeAndIndexExactValue(Node node, String key, String value, Index<Node> index) {
        // store
        node.setProperty(key, value);
        // index
        indexNodeValue(node, value, asList(IndexMode.KEY), key, index, null);   // fulltextIndex=null
    }

    // ---

    private void storeAndIndexTopicValue(Node topicNode, Object value, Collection<IndexMode> indexModes,
                                                                       String indexKey, Object indexValue) {
        // store
        topicNode.setProperty(KEY_VALUE, value);
        // index
        indexNodeValue(topicNode, indexValue, indexModes, indexKey, topicContentExact, topicContentFulltext);
    }

    private void storeAndIndexAssociationValue(Node assocNode, Object value, Collection<IndexMode> indexModes,
                                                                             String indexKey, Object indexValue) {
        // store
        assocNode.setProperty(KEY_VALUE, value);
        // index
        indexNodeValue(assocNode, indexValue, indexModes, indexKey, assocContentExact, assocContentFulltext);
    }

    // ---

    private void removeTopicFromIndex(Node topicNode) {
        topicContentExact.remove(topicNode);
        topicContentFulltext.remove(topicNode);
    }

    private void removeAssociationFromIndex(Node assocNode) {
        assocContentExact.remove(assocNode);
        assocContentFulltext.remove(assocNode);
    }

    // ---

    private Object getIndexValue(SimpleValue value, SimpleValue indexValue) {
        return indexValue != null ? indexValue.value() : value.value();
    }



    // === Indexing ===

    private void indexNodeValue(Node node, Object value, Collection<IndexMode> indexModes, String indexKey,
                                                         Index<Node> exactIndex, Index<Node> fulltextIndex) {
        for (IndexMode indexMode : indexModes) {
            if (indexMode == IndexMode.OFF) {
                return;
            } else if (indexMode == IndexMode.KEY) {
                exactIndex.remove(node, indexKey);              // remove old
                exactIndex.add(node, indexKey, value);          // index new
            } else if (indexMode == IndexMode.FULLTEXT) {
                fulltextIndex.remove(node, KEY_FULLTEXT);       // remove old
                fulltextIndex.add(node, KEY_FULLTEXT, value);   // index new
            } else if (indexMode == IndexMode.FULLTEXT_KEY) {
                fulltextIndex.remove(node, indexKey);           // remove old
                fulltextIndex.add(node, indexKey, value);       // index new
            } else {
                throw new RuntimeException("Unexpected index mode: \"" + indexMode + "\"");
            }
        }
    }

    // ---

    private void indexAssociation(Node assocNode, String roleTypeUri1, Node playerNode1,
                                                  String roleTypeUri2, Node playerNode2) {
        indexAssociationId(assocNode);
        indexAssociationType(assocNode, typeUri(assocNode));
        //
        indexAssociationRole(assocNode, 1, roleTypeUri1, playerNode1);
        indexAssociationRole(assocNode, 2, roleTypeUri2, playerNode2);
    }

    private void indexAssociationId(Node assocNode) {
        assocMetadata.add(assocNode, KEY_ASSOC_ID, assocNode.getId());
    }

    private void indexAssociationType(Node assocNode, String assocTypeUri) {
        reindexValue(assocNode, KEY_ASSOC_TPYE_URI, assocTypeUri);
    }

    private void indexAssociationRole(Node assocNode, int pos, String roleTypeUri, Node playerNode) {
        assocMetadata.add(assocNode, KEY_ROLE_TPYE_URI + pos, roleTypeUri);
        assocMetadata.add(assocNode, KEY_PLAYER_TPYE + pos, NodeType.of(playerNode).stringify());
        assocMetadata.add(assocNode, KEY_PLAYER_ID + pos, playerNode.getId());
        assocMetadata.add(assocNode, KEY_PLAYER_TYPE_URI + pos, typeUri(playerNode));
    }

    // ---

    private void indexAssociationRoleType(Node assocNode, long playerId, String roleTypeUri) {
        int pos = lookupPlayerPosition(assocNode.getId(), playerId);
        reindexValue(assocNode, KEY_ROLE_TPYE_URI, pos, roleTypeUri);
    }

    private int lookupPlayerPosition(long assocId, long playerId) {
        boolean pos1 = isPlayerAtPosition(1, assocId, playerId);
        boolean pos2 = isPlayerAtPosition(2, assocId, playerId);
        if (pos1 && pos2) {
            throw new RuntimeException("Ambiguity: both players have ID " + playerId + " in association " + assocId);
        } else if (pos1) {
            return 1;
        } else if (pos2) {
            return 2;
        } else {
            throw new IllegalArgumentException("ID " + playerId + " is not a player in association " + assocId);
        }
    }

    private boolean isPlayerAtPosition(int pos, long assocId, long playerId) {
        BooleanQuery query = new BooleanQuery();
        addTermQuery(KEY_ASSOC_ID, assocId, query);
        addTermQuery(KEY_PLAYER_ID + pos, playerId, query);
        return assocMetadata.query(query).getSingle() != null;
    }

    // ---

    private void reindexTypeUri(Node playerNode, String typeUri) {
        reindexTypeUri(1, playerNode, typeUri);
        reindexTypeUri(2, playerNode, typeUri);
    }

    /**
     * Re-indexes the KEY_PLAYER_TYPE_URI of all associations in which the specified node
     * is a player at the specified position.
     *
     * @param   playerNode  a topic node or an association node.
     * @param   typeUri     the new type URI to be indexed for the player node.
     */
    private void reindexTypeUri(int pos, Node playerNode, String typeUri) {
        for (Node assocNode : lookupAssociations(pos, playerNode)) {
            reindexValue(assocNode, KEY_PLAYER_TYPE_URI, pos, typeUri);
        }
    }

    private IndexHits<Node> lookupAssociations(int pos, Node playerNode) {
        return assocMetadata.get(KEY_PLAYER_ID + pos, playerNode.getId());
    }

    // ---

    private void reindexValue(Node assocNode, String key, int pos, String value) {
        reindexValue(assocNode, key + pos, value);
    }

    private void reindexValue(Node assocNode, String key, String value) {
        assocMetadata.remove(assocNode, key);
        assocMetadata.add(assocNode, key, value);
    }

    // ---

    private Set<AssociationModel> queryAssociationIndex(String assocTypeUri,
                                     String roleTypeUri1, NodeType playerType1, long playerId1, String playerTypeUri1,
                                     String roleTypeUri2, NodeType playerType2, long playerId2, String playerTypeUri2) {
        return executeAssociationQuery(buildAssociationQuery(assocTypeUri,
            roleTypeUri1, playerType1, playerId1, playerTypeUri1,
            roleTypeUri2, playerType2, playerId2, playerTypeUri2
        ));
    }

    // ---

    private Query buildAssociationQuery(String assocTypeUri,
                                     String roleTypeUri1, NodeType playerType1, long playerId1, String playerTypeUri1,
                                     String roleTypeUri2, NodeType playerType2, long playerId2, String playerTypeUri2) {
        // query bidirectional
        BooleanQuery direction1 = new BooleanQuery();
        addRole(direction1, 1, roleTypeUri1, playerType1, playerId1, playerTypeUri1);
        addRole(direction1, 2, roleTypeUri2, playerType2, playerId2, playerTypeUri2);
        BooleanQuery direction2 = new BooleanQuery();
        addRole(direction2, 1, roleTypeUri2, playerType2, playerId2, playerTypeUri2);
        addRole(direction2, 2, roleTypeUri1, playerType1, playerId1, playerTypeUri1);
        //
        BooleanQuery roleQuery = new BooleanQuery();
        roleQuery.add(direction1, Occur.SHOULD);
        roleQuery.add(direction2, Occur.SHOULD);
        //
        BooleanQuery query = new BooleanQuery();
        if (assocTypeUri != null) {
            addTermQuery(KEY_ASSOC_TPYE_URI, assocTypeUri, query);
        }
        query.add(roleQuery, Occur.MUST);
        //
        return query;
    }

    private void addRole(BooleanQuery query, int pos, String roleTypeUri, NodeType playerType, long playerId,
                                                                                               String playerTypeUri) {
        if (roleTypeUri != null)   addTermQuery(KEY_ROLE_TPYE_URI + pos,   roleTypeUri,   query);
        if (playerType != null)    addTermQuery(KEY_PLAYER_TPYE + pos,     playerType,    query);
        if (playerId != -1)        addTermQuery(KEY_PLAYER_ID + pos,       playerId,      query);
        if (playerTypeUri != null) addTermQuery(KEY_PLAYER_TYPE_URI + pos, playerTypeUri, query);
    }

    // ---

    private void addTermQuery(String key, long value, BooleanQuery query) {
        addTermQuery(key, Long.toString(value), query);
    }

    private void addTermQuery(String key, NodeType nodeType, BooleanQuery query) {
        addTermQuery(key, nodeType.stringify(), query);
    }

    private void addTermQuery(String key, String value, BooleanQuery query) {
        query.add(new TermQuery(new Term(key, value)), Occur.MUST);
    }

    // ---

    private Set<AssociationModel> executeAssociationQuery(Query query) {
        Set<AssociationModel> assocs = new HashSet();
        for (Node assocNode : assocMetadata.query(query)) {
            assocs.add(buildAssociation(assocNode));
        }
        return assocs;
    }

    // ---

    private Index<Node> createExactIndex(String name) {
        return neo4j.index().forNodes(name);
    }

    private Index<Node> createFulltextIndex(String name) {
        if (neo4j.index().existsForNodes(name)) {
            return neo4j.index().forNodes(name);
        } else {
            Map<String, String> configuration = stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext");
            return neo4j.index().forNodes(name, configuration);
        }
    }
}
