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
import static org.neo4j.helpers.collection.MapUtil.stringMap;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;

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

    // ### TODO: define further string constants for key names etc.
    private static final String KEY_FULLTEXT = "_fulltext";

    // association metadata
    private static final String KEY_ASSOC_TPYE_URI = "assoc_type_uri";
    // role 1
    private static final String KEY_ROLE_TPYE_URI_1   = "role_type_uri_1";
    private static final String KEY_PLAYER_TPYE_1     = "player_type_1";
    private static final String KEY_PLAYER_ID_1       = "player_id_1";
    private static final String KEY_PLAYER_TYPE_URI_1 = "player_type_uri_1";
    // role 2
    private static final String KEY_ROLE_TPYE_URI_2   = "role_type_uri_2";
    private static final String KEY_PLAYER_TPYE_2     = "player_type_2";
    private static final String KEY_PLAYER_ID_2       = "player_id_2";
    private static final String KEY_PLAYER_TYPE_URI_2 = "player_type_uri_2";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private GraphDatabaseService neo4j = null;
    private Neo4jRelationtypeCache relTypeCache;
    
    private Index<Node> topicContentExact;
    private Index<Node> topicContentFulltext;
    private Index<Node> assocContentExact;
    private Index<Node> assocContentFulltext;
    private Index<Node> assocMetadata;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Neo4jStorage(String databasePath) {
        try {
            this.neo4j = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);
            this.relTypeCache = new Neo4jRelationtypeCache(neo4j);
            // indexes
            this.topicContentExact    = createExactIndex("topic-content-exact");
            this.topicContentFulltext = createFulltextIndex("topic-content-fulltext");
            this.assocContentExact    = createExactIndex("assoc-content-exact");
            this.assocContentFulltext = createFulltextIndex("assoc-content-fulltext");
            this.assocMetadata = createExactIndex("assoc-metadata");
        } catch (Exception e) {
            if (neo4j != null) {
                logger.info("Shutdown Neo4j");
                neo4j.shutdown();
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

    @Override
    public void storeTopicValue(long topicId, SimpleValue value, Collection<IndexMode> indexModes, String indexKey) {
        storeAndIndexTopicValue(fetchTopicNode(topicId), value.value(), indexModes, indexKey);
    }

    // ---

    @Override
    public void storeTopic(TopicModel topicModel) {
        String uri = topicModel.getUri();
        checkUriUniqueness(uri);
        //
        // 1) update DB
        Node topicNode = neo4j.createNode();
        topicNode.setProperty("node_type", "topic");
        //
        storeAndIndexTopicUri(topicNode, uri);
        storeAndIndexTopicTypeUri(topicNode, topicModel.getTypeUri());
        //
        // 2) update model
        topicModel.setId(topicNode.getId());
    }

    @Override
    public void deleteTopic(long topicId) {
        fetchTopicNode(topicId).delete();
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
        Set<AssociationModel> assocs = new HashSet();
        Query query = buildAssociationQuery(assocTypeUri,
            roleTypeUri1, NodeType.TOPIC, topicId1, null,
            roleTypeUri2, NodeType.TOPIC, topicId2, null);
        for (Node assocNode : assocMetadata.query(query)) {
            assocs.add(buildAssociation(assocNode));
        }
        return assocs;
    }

    @Override
    public Set<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        Set<AssociationModel> assocs = new HashSet();
        Query query = buildAssociationQuery(assocTypeUri,
            topicRoleTypeUri, NodeType.TOPIC, topicId, null,
            assocRoleTypeUri, NodeType.ASSOC, assocId, null);
        for (Node assocNode : assocMetadata.query(query)) {
            assocs.add(buildAssociation(assocNode));
        }
        return assocs;
    }

    // ---

    @Override
    public void storeAssociationUri(long assocId, String uri) {
        storeAndIndexAssociationUri(fetchAssociationNode(assocId), uri);
    }

    @Override
    public void storeAssociationValue(long assocId, SimpleValue value, Collection<IndexMode> indexModes,
                                                                                      String indexKey) {
        storeAndIndexAssociationValue(fetchAssociationNode(assocId), value.value(), indexModes, indexKey);
    }

    @Override
    public void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
        Node assocNode = fetchAssociationNode(assocId);
        fetchRelationship(assocNode, playerId).delete();
        assocNode.createRelationshipTo(fetchNode(playerId), getRelationshipType(roleTypeUri));
    }

    // ---

    @Override
    public void storeAssociation(AssociationModel assocModel) {
        String uri = assocModel.getUri();
        checkUriUniqueness(uri);
        //
        // 1) update DB
        Node assocNode = neo4j.createNode();
        assocNode.setProperty("node_type", "assoc");
        //
        storeAndIndexAssociationUri(assocNode, uri);
        storeAndIndexAssociationTypeUri(assocNode, assocModel.getTypeUri());
        //
        RoleModel role1 = assocModel.getRoleModel1();
        RoleModel role2 = assocModel.getRoleModel2();
        Node playerNode1 = storePlayerRelationship(assocNode, role1);
        Node playerNode2 = storePlayerRelationship(assocNode, role2);
        //
        // index for bidirectional retrieval
        String roleTypeUri1 = role1.getRoleTypeUri();
        String roleTypeUri2 = role2.getRoleTypeUri();
        indexAssociation(assocNode, roleTypeUri1, playerNode1, roleTypeUri2, playerNode2);
        indexAssociation(assocNode, roleTypeUri2, playerNode2, roleTypeUri1, playerNode1);
        //
        // 2) update model
        assocModel.setId(assocNode.getId());
    }

    @Override
    public void deleteAssociation(long assocId) {
        Node assocNode = fetchAssociationNode(assocId);
        // delete the 2 player relationships
        for (Relationship rel : fetchRelationships(assocNode)) {
            rel.delete();
        }
        //
        assocNode.delete();
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
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.TOPIC, topicId, null,
            othersRoleTypeUri, NodeType.TOPIC, -1, othersTopicTypeUri);
        return buildRelatedTopics(queryAssociations(query), topicId);
    }

    @Override
    public Set<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.TOPIC, topicId, null,
            othersRoleTypeUri, NodeType.ASSOC, -1, othersAssocTypeUri);
        return buildRelatedAssociations(queryAssociations(query), topicId);
    }

    // ---

    @Override
    public Set<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                String othersRoleTypeUri, String othersTopicTypeUri) {
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.ASSOC, assocId, null,
            othersRoleTypeUri, NodeType.TOPIC, -1, othersTopicTypeUri);
        return buildRelatedTopics(queryAssociations(query), assocId);
    }

    @Override
    public Set<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        // ### TODO: respect maxResultSize
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.ASSOC, assocId, null,
            othersRoleTypeUri, NodeType.ASSOC, -1, othersAssocTypeUri);
        return buildRelatedAssociations(queryAssociations(query), assocId);
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
            if (rootNode.getProperty("node_type", null) != null) {
                return false;
            }
            //
            rootNode.setProperty("node_type", "topic");
            storeAndIndexTopicUri(rootNode, "dm4.core.meta_type");
            storeAndIndexTopicTypeUri(rootNode, "dm4.core.meta_meta_type");
            rootNode.setProperty("value", "Meta Type");
            //
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Setting up the root node (0) failed", e);
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutdown DB");
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
            throw new RuntimeException("Data inconsistency: association " + assocNode.getId() +
                " connects " + rels.size() + " player instead of 2");
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
        return checkType(
            topicContentExact.get("uri", uri).getSingle(), NodeType.TOPIC
        );
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
        return (String) node.getProperty("uri");
    }

    private String typeUri(Node node) {
        return (String) node.getProperty("type_uri");
    }

    private SimpleValue simpleValue(Node node) {
        return new SimpleValue(node.getProperty("value"));
    }



    // === DeepaMehta Helper ===

    private Set<RelatedTopicModel> buildRelatedTopics(Collection<AssociationModel> assocs, long playerId) {
        Set<RelatedTopicModel> relTopics = new HashSet();
        for (AssociationModel assoc : assocs) {
            long relatedTopicId = assoc.getOtherRoleModel(playerId).getPlayerId();
            TopicModel relatedTopic = fetchTopic(relatedTopicId);
            relTopics.add(new RelatedTopicModel(relatedTopic, assoc));
        }
        return relTopics;
    }

    private Set<RelatedAssociationModel> buildRelatedAssociations(Collection<AssociationModel> assocs, long playerId) {
        Set<RelatedAssociationModel> relAssocs = new HashSet();
        for (AssociationModel assoc : assocs) {
            long relatedAssocId = assoc.getOtherRoleModel(playerId).getPlayerId();
            AssociationModel relatedAssoc = fetchAssociation(relatedAssocId);
            relAssocs.add(new RelatedAssociationModel(relatedAssoc, assoc));
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
        Node n1 = topicContentExact.get("uri", uri).getSingle();
        Node n2 = assocContentExact.get("uri", uri).getSingle();
        if (n1 != null || n2 != null) {
            throw new RuntimeException("URI \"" + uri + "\" is not unique");
        }
    }

    // ---

    private List<AssociationModel> queryAssociations(Query query) {
        List<AssociationModel> assocs = new ArrayList();
        for (Node assocNode : assocMetadata.query(query)) {
            assocs.add(buildAssociation(assocNode));
        }
        return assocs;
    }



    // === Value Storage ===

    private void storeAndIndexTopicUri(Node topicNode, String uri) {
        storeAndIndexExactValue(topicNode, "uri", uri, topicContentExact);
    }

    private void storeAndIndexAssociationUri(Node assocNode, String uri) {
        storeAndIndexExactValue(assocNode, "uri", uri, assocContentExact);
    }

    // ---

    private void storeAndIndexTopicTypeUri(Node topicNode, String typeUri) {
        storeAndIndexExactValue(topicNode, "type_uri", typeUri, topicContentExact);
    }

    private void storeAndIndexAssociationTypeUri(Node assocNode, String typeUri) {
        storeAndIndexExactValue(assocNode, "type_uri", typeUri, assocContentExact);
    }

    // ---

    private void storeAndIndexExactValue(Node node, String key, String value, Index<Node> index) {
        node.setProperty(key, value);
        indexNodeValue(node, value, asList(IndexMode.KEY), key, index, null);   // fulltextIndex=null
    }

    // ---

    private void storeAndIndexTopicValue(Node topicNode, Object value, Collection<IndexMode> indexModes,
                                                                       String indexKey) {
        topicNode.setProperty("value", value);
        indexNodeValue(topicNode, value, indexModes, indexKey, topicContentExact, topicContentFulltext);
    }

    private void storeAndIndexAssociationValue(Node assocNode, Object value, Collection<IndexMode> indexModes,
                                                                             String indexKey) {
        assocNode.setProperty("value", value);
        indexNodeValue(assocNode, value, indexModes, indexKey, assocContentExact, assocContentFulltext);
    }



    // === Indexing ===

    private void indexNodeValue(Node node, Object value, Collection<IndexMode> indexModes, String indexKey,
                                                         Index<Node> exactIndex, Index<Node> fulltextIndex) {
        // ### TODO: strip HTML tags before indexing
        /*if (getType().getDataTypeUri().equals("dm4.core.html")) {
            value = new SimpleValue(JavaUtils.stripHTML(value.toString()));
        }*/
        //
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


    private void indexAssociation(Node assocNode, String roleTypeUri1, Node playerNode1,
                                                  String roleTypeUri2, Node playerNode2) {
        assocMetadata.add(assocNode, KEY_ASSOC_TPYE_URI, typeUri(assocNode));
        // role 1
        assocMetadata.add(assocNode, KEY_ROLE_TPYE_URI_1, roleTypeUri1);
        assocMetadata.add(assocNode, KEY_PLAYER_TPYE_1, NodeType.of(playerNode1).stringify());
        assocMetadata.add(assocNode, KEY_PLAYER_ID_1, playerNode1.getId());
        assocMetadata.add(assocNode, KEY_PLAYER_TYPE_URI_1, typeUri(playerNode1));
        // role 2
        assocMetadata.add(assocNode, KEY_ROLE_TPYE_URI_2, roleTypeUri2);
        assocMetadata.add(assocNode, KEY_PLAYER_TPYE_2, NodeType.of(playerNode2).stringify());
        assocMetadata.add(assocNode, KEY_PLAYER_ID_2, playerNode2.getId());
        assocMetadata.add(assocNode, KEY_PLAYER_TYPE_URI_2, typeUri(playerNode2));
    }

    private Query buildAssociationQuery(String assocTypeUri,
                                     String roleTypeUri1, NodeType playerType1, long playerId1, String playerTypeUri1,
                                     String roleTypeUri2, NodeType playerType2, long playerId2, String playerTypeUri2) {
        BooleanQuery query = new BooleanQuery();
        //
        if (assocTypeUri != null)   addTermQuery(KEY_ASSOC_TPYE_URI,    assocTypeUri,             query);
        // role 1
        if (roleTypeUri1 != null)   addTermQuery(KEY_ROLE_TPYE_URI_1,   roleTypeUri1,             query);
        if (playerType1 != null)    addTermQuery(KEY_PLAYER_TPYE_1,     playerType1.stringify(),  query);
        if (playerId1 != -1)        addTermQuery(KEY_PLAYER_ID_1,       Long.toString(playerId1), query);
        if (playerTypeUri1 != null) addTermQuery(KEY_PLAYER_TYPE_URI_1, playerTypeUri1,           query);
        // role 2
        if (roleTypeUri2 != null)   addTermQuery(KEY_ROLE_TPYE_URI_2,   roleTypeUri2,             query);
        if (playerType2 != null)    addTermQuery(KEY_PLAYER_TPYE_2,     playerType2.stringify(),  query);
        if (playerId2 != -1)        addTermQuery(KEY_PLAYER_ID_2,       Long.toString(playerId2), query);
        if (playerTypeUri2 != null) addTermQuery(KEY_PLAYER_TYPE_URI_2, playerTypeUri2,           query);
        //
        return query;
    }

    private void addTermQuery(String key, String value, BooleanQuery query) {
        query.add(new TermQuery(new Term(key, value)), Occur.MUST);
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
