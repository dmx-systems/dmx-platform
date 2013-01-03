package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.storage.MehtaObjectRole;
import de.deepamehta.core.storage.spi.MehtaEdge;
import de.deepamehta.core.storage.spi.MehtaGraph;
import de.deepamehta.core.storage.spi.MehtaGraphTransaction;
import de.deepamehta.core.storage.spi.MehtaNode;
import de.deepamehta.core.storage.spi.MehtaObject;

import org.neo4j.graphdb.Node;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public class Neo4jMehtaGraph extends Neo4jBase implements MehtaGraph {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Neo4jMehtaGraph(String databasePath) {
        super(databasePath);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *********************************
    // *** MehtaGraph Implementation ***
    // *********************************



    // === Topics ===

    @Override
    public void createMehtaNode(TopicModel topicModel) {
        String uri = topicModel.getUri();
        checkTopicUniqueness(uri);
        // 1) update DB
        Node node = neo4j.createNode();
        node.setProperty("node_type", "topic");
        storeAndIndexTopicUri(node, uri);
        storeAndIndexTopicTypeUri(node, topicModel.getTypeUri());
        // 2) update model
        topicModel.setId(node.getId());
    }

    // ---

    @Override
    public TopicModel getMehtaNode(long id) {
        return buildTopic(fetchTopicNode(topicId));
    }

    @Override
    public TopicModel getMehtaNode(String key, Object value) {
        Node node = topicContentExact.get(key, value).getSingle();
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
    public List<TopicModel> getMehtaNodes(String key, Object value) {
        return buildTopics(topicContentExact.query(key, value));
    }

    // ---

    @Override
    public List<TopicModel> queryMehtaNodes(Object value) {
        return queryMehtaNodes(null, value);
    }

    @Override
    public List<TopicModel> queryMehtaNodes(String key, Object value) {
        if (key == null) {
            key = KEY_FULLTEXT;
        }
        if (value == null) {
            throw new IllegalArgumentException("Tried to call queryMehtaNodes() with a null value Object (key=\"" +
                key + "\")");
        }
        //
        return buildTopics(topicContentFulltext.query(key, value));
    }

    // ---

    @Override
    public void setTopicUri(long topicId, String uri) {
        storeAndIndexTopicUri(fetchTopicNode(topicId), uri);
    }

    @Override
    public void setTopicValue(long topicId, SimpleValue value, Set<IndexMode> indexModes, String indexKey) {
        storeAndIndexTopicValue(fetchTopicNode(topicId), value.value(), indexModes, indexKey);
    }

    // ---

    @Override
    public Object getTopicProperty(long topicId, String key) {
        return fetchTopicNode(topicId).getProperty(key);
    }

    @Override
    public void setTopicProperty(long topicId, String key, Object value) {
        fetchTopicNode(topicId).setProperty(key, value);
    }

    @Override
    public boolean hasTopicProperty(long topicId, String key) {
        return fetchTopicNode(topicId).hasProperty(key);
    }

    // ---

    @Override
    public void deleteTopic(long topicId) {
        fetchTopicNode(topicId).delete();
    }



    // === Associations ===

    @Override
    public MehtaEdge createMehtaEdge(MehtaObjectRole object1, MehtaObjectRole object2) {
        Node auxiliaryNode = neo4j.createNode();
        auxiliaryNode.setProperty(KEY_IS_MEHTA_EDGE, true);
        Neo4jMehtaEdge mehtaEdge = buildMehtaEdge(auxiliaryNode);
        //
        mehtaEdge.addMehtaObject(object1);
        mehtaEdge.addMehtaObject(object2);
        return mehtaEdge;
    }

    // ---

    @Override
    public AssociationModel getMehtaEdge(long assocId) {
        return buildAssociation(fetchAssociationNode(assocId));
    }

    // ---

    @Override
    public Set<AssociationModel> getMehtaEdges(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                                  String roleTypeUri2) {
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
    public Set<AssociationModel> getMehtaEdgesBetweenNodeAndEdge(String assocTypeUri, long topicId, long assocId,
                                                                 String topicRoleTypeUri, String assocRoleTypeUri) {
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
        storeAndIndexAssociationUri(fetchAssociationNode(topicId), uri);
    }

    @Override
    public void storeAssociationValue(long assocId, SimpleValue value, Set<IndexMode> indexModes, String indexKey) {
        storeAndIndexAssociationValue(fetchAssociationNode(assocId), value.value(), indexModes, indexKey);
    }

    @Override
    public void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
        Node assocNode = fetchAssociationNode(assocId);
        fetchRelationship(assocNode, playerId).delete();
        assocNode.createRelationshipTo(playerId, getRelationshipType(roleTypeUri));
    }



    // === Mehta Objects ### TODO ===

    @Override
    public MehtaObject getMehtaObject(long id) {
        return buildMehtaObject(neo4j.getNodeById(id));
    }



    // === Traversal ===

    @Override
    public Set<AssociationModel> getTopicAssociations(long topicId) {
        return fetchAssociations(fetchTopicNode(topicId));
    }

    @Override
    public Set<AssociationModel> getAssociationAssociations(long assocId) {
        return fetchAssociations(fetchAssociationNode(assocId));
    }

    // ---

    @Override
    public Set<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                        String othersRoleTypeUri, String othersTopicTypeUri) {
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.TOPIC, topicId1, null,
            othersRoleTypeUri, NodeType.TOPIC, -1, othersTopicTypeUri);
        return buildRelatedTopics(queryAssociations(query));
    }

    @Override
    public Set<RelatedAssociationModel> getTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.TOPIC, topicId1, null,
            othersRoleTypeUri, NodeType.ASSOC, -1, othersAssocTypeUri);
        return buildRelatedAssociations(queryAssociations(query));
    }

    // ---

    @Override
    public Set<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.ASSOC, assocId, null,
            othersRoleTypeUri, NodeType.TOPIC, -1, othersTopicTypeUri);
        return buildRelatedTopics(queryAssociations(query));
    }

    @Override
    public Set<RelatedAssociationModel> getAssociationRelatedAssociations(long assocId, String assocTypeUri,
                         String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        // ### TODO: respect maxResultSize
        Query query = buildAssociationQuery(assocTypeUri,
            myRoleTypeUri,     NodeType.ASSOC, assocId, null,
            othersRoleTypeUri, NodeType.ASSOC, -1, othersAssocTypeUri);
        return buildRelatedAssociations(queryAssociations(query));
    }



    // === Misc ===

    @Override
    public MehtaGraphTransaction beginTx() {
        return new Neo4jTransactionAdapter(neo4j);
    }

    @Override
    public void shutdown() {
        logger.info("Shutdown DB");
        neo4j.shutdown();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicModel buildTopic(Node node) {
        long id = node.getId();
        String uri = node.getProperty("uri");
        String typeUri = node.getProperty("type_uri");
        SimpleValue value = new SimpleValue(node.getProperty("value"));
        return new TopicModel(id, uri, typeUri, value, null);   // composite=null
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
        long id = assocNode.getId();
        String uri = assocNode.getProperty("uri");
        String typeUri = assocNode.getProperty("type_uri");;
        SimpleValue value = new SimpleValue(assocNode.getProperty("value"));
        List<RoleModel> roleModels = buildRoleModels(assocNode);
        return new AssociationModel(id, uri, typeUri, roleModels.get(0), roleModels.get(1), value, null);
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

    // ---

    private Set<RelatedTopicModel> buildRelatedTopics(Collection<AssociationModel> assocs, long playerId) {
        Set<RelatedTopicModel> relTopics = new HashSet();
        for (AssociationModel assoc : assocs) {
            long relatedTopicId = assoc.getOtherRoleModel(playerId).getPlayerId();
            TopicModel relatedTopic = getMehtaNode(relatedTopicId);
            relTopics.add(new RelatedTopicModel(relatedTopic, assoc));
        }
        return relTopics;
    }

    private Set<RelatedAssociationModel> buildRelatedAssociations(Collection<AssociationModel> assocs, long playerId) {
        Set<RelatedAssociationModel> relAssocs = new HashSet();
        for (AssociationModel assoc : assocs) {
            long relatedAssocId = assoc.getOtherRoleModel(playerId).getPlayerId();
            AssociationModel relatedAssoc = getMehtaEdge(relatedAssocId);
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
    private void checkTopicUniqueness(String uri) {
        // ### TODO: check both, topic index and assoc index. URI must be unique globally.
        // ### TODO: just query index, don't build topic model
        if (!uri.equals("") && lookupMehtaNode(uri) != null) {
            throw new RuntimeException("Topic URI \"" + uri + "\" is not unique");
        }
    }

    private MehtaNode lookupMehtaNode(String uri) {
        return getMehtaNode("uri", uri);
    }

    // ---

    private Node fetchTopicNode(long topicId) {
        Node node = neo4j.getNodeById(topicId);
        checkType(node, NodeType.TOPIC);
        return node;
    }

    private Node fetchAssociationNode(long assocId) {
        Node node = neo4j.getNodeById(assocId);
        checkType(node, NodeType.ASSOCIATION);
        return node;
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

    // ---

    private void checkType(Node node, NodeType type) {
        if (NodeType.of(node) != type) {
            throw new IllegalArgumentException(type.error(node));
        }
    }

    // ---

    private void storeAndIndexTopicUri(Node topicNode, String uri) {
        topicNode.setProperty("uri", uri);
        indexNodeValue(topicNode, uri, IndexMode.KEY, "uri", topicContentExact, null);          // fulltextIndex=null
    }

    private void storeAndIndexAssociationUri(Node assocNode, String uri) {
        assocNode.setProperty("uri", uri);
        indexNodeValue(assocNode, uri, IndexMode.KEY, "uri", assocContentExact, null);          // fulltextIndex=null
    }

    // ---

    private void storeAndIndexTopicTypeUri(Node topicNode, String typeUri) {
        topicNode.setProperty("type_uri", typeUri);
        indexNodeValue(topicNode, typeUri, IndexMode.KEY, "type_uri", topicContentExact, null); // fulltextIndex=null
    }

    private void storeAndIndexAssociationTypeUri(Node assocNode, String typeUri) {
        assocNode.setProperty("type_uri", typeUri);
        indexNodeValue(assocNode, typeUri, IndexMode.KEY, "type_uri", assocContentExact, null); // fulltextIndex=null
    }

    // ---

    private void storeAndIndexTopicValue(Node topicNode, Object value, Set<IndexMode> indexModes,
                                                                       String indexKey) {
        topicNode.setProperty("value", value);
        indexNodeValue(topicNode, value, indexModes, indexKey, topicContentExact, topicContentFulltext);
    }

    private void storeAndIndexAssociationValue(Node assocNode, Object value, Set<IndexMode> indexModes,
                                                                             String indexKey) {
        assocNode.setProperty("value", value);
        indexNodeValue(assocNode, value, indexModes, indexKey, assocContentExact, assocContentFulltext);
    }

    // ---

    private void indexNodeValue(Node node, Object value, Set<IndexMode> indexModes, String indexKey,
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
                throw new RuntimeException("Index mode \"" + indexMode + "\" not implemented");
            }
        }
    }

    // ---

    private Query buildAssociationQuery(String assocTypeUri,
                                    String roleTypeUri1, NodeType playerType1, long playerId1, String playerTypeUri1,
                                    String roleTypeUri2, NodeType playerType2, long playerId2, String playerTypeUri2) {
        Query query = new BooleanQuery();
        query.add(new TermQuery(new Term("assoc_type_uri", assocTypeUri)), Occur.MUST);
        // ### TODO
        return query;
    }

    private List<AssociationModel> queryAssociations(Query query) {
        List<AssociationModel> assocs = new ArrayList();
        for (Node assocNode : assocMetadata.query(query)) {
            assocs.add(buildAssociation(assocNode));
        }
        return assocs;
    }
}
