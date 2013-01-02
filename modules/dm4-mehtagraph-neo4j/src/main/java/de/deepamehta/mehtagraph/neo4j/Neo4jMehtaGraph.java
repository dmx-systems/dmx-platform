package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.core.ResultSet;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public class Neo4jMehtaGraph extends Neo4jBase implements MehtaGraph {

    private enum NodeType {

        TOPIC {
            @Override
            RoleModel createRoleModel(Node node, String roleTypeUri) {
                return new TopicRoleModel(node.getId(), roleTypeUri);
            }

            @Override
            String error(Node node) {
                return "ID " + node.getId() + " refers to an Association when the caller expects a Topic";
            }
        },
        ASSOCIATION {
            @Override
            RoleModel createRoleModel(Node node, String roleTypeUri) {
                return new AssociationRoleModel(node.getId(), roleTypeUri);
            }

            @Override
            String error(Node node) {
                return "ID " + node.getId() + " refers to a Topic when the caller expects an Association";
            }
        };

        private static NodeType of(Node node) {
            String type = node.getProperty("node_type");
            return valueOf(type.toUpperCase());
        }

        // ---

        abstract RoleModel createRoleModel(long nodeId, String roleTypeUri);

        abstract String error();
    }

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
        storeAndIndexUri(node, uri);
        storeAndIndexTypeUri(node, topicModel.getTypeUri());
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
        Node node = exactNodeIndex.get(key, value).getSingle();
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
    public List<TopicModel> getMehtaNodes(String key, Object value) {
        return buildTopics(exactNodeIndex.query(key, value));
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
        return buildTopics(fulltextNodeIndex.query(key, value));
    }

    // ---

    @Override
    public void setTopicUri(long topicId, String uri) {
        storeAndIndexUri(fetchTopicNode(topicId), uri);
    }

    @Override
    public void setTopicValue(long topicId, SimpleValue value, IndexMode indexMode, String indexKey) {
        storeAndIndexValue(fetchTopicNode(topicId), value.value(), indexMode, indexKey);
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
    public MehtaEdge getMehtaEdge(long id) {
        return buildMehtaEdge(neo4j.getNodeById(id));
    }

    // ---

    @Override
    public Set<MehtaEdge> getMehtaEdges(long node1Id, long node2Id) {
        return new TraveralResultBuilder(getMehtaNode(node1Id), traverseToMehtaNode(node2Id)) {
            @Override
            Object buildResult(Node connectedNode, Node auxiliaryNode) {
                return buildMehtaEdge(auxiliaryNode);
            }
        }.getResult();
    }

    @Override
    public Set<MehtaEdge> getMehtaEdges(long node1Id, long node2Id, String roleType1, String roleType2) {
        return new TraveralResultBuilder(getMehtaNode(node1Id), traverseToMehtaNode(node2Id, roleType1,
                                                                                             roleType2)) {
            @Override
            Object buildResult(Node connectedNode, Node auxiliaryNode) {
                return buildMehtaEdge(auxiliaryNode);
            }
        }.getResult();
    }

    @Override
    public Set<MehtaEdge> getMehtaEdgesBetweenNodeAndEdge(long nodeId, long edgeId, String nodeRoleType,
                                                                                    String edgeRoleType) {
        return new TraveralResultBuilder(getMehtaNode(nodeId), traverseToMehtaEdge(edgeId, nodeRoleType,
                                                                                           edgeRoleType)) {
            @Override
            Object buildResult(Node connectedNode, Node auxiliaryNode) {
                return buildMehtaEdge(auxiliaryNode);
            }
        }.getResult();
    }



    // === Mehta Objects ### TODO ===

    @Override
    public MehtaObject getMehtaObject(long id) {
        return buildMehtaObject(neo4j.getNodeById(id));
    }



    // === Traversal ===

    @Override
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                            String othersRoleTypeUri, String othersTopicTypeUri, int maxResultSize) {
        // ### TODO: respect maxResultSize
        Set<RelatedTopicModel> relTopics = new HashSet();
        String indexValue = indexValue(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        for (Node middleNode : associationIndex.get("related_topic", indexValue)) {
            AssociationModel assoc = buildAssociation(middleNode);
            long relatedTopicId = assoc.getOtherRoleModel(topicId).getPlayerId();
            TopicModel relatedTopic = getMehtaNode(relatedTopicId);
            relTopics.add(new RelatedTopicModel(relatedTopic, assoc));
        }
        return new ResultSet(relTopics.size(), relTopics);
    }

    @Override
    public Set<RelatedAssociationModel> getTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        // ### TODO: respect maxResultSize
        Set<RelatedAssociationModel> relAssocs = new HashSet();
        String indexValue = indexValue(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        for (Node middleNode : associationIndex.get("related_assoc", indexValue)) {
            AssociationModel assoc = buildAssociation(middleNode);
            long relatedAssocId = assoc.getOtherRoleModel(topicId).getPlayerId();
            AssociationModel relatedAssoc = getMehtaEdge(relatedAssocId);
            relAssocs.add(new RelatedAssociationModel(relatedAssoc, assoc));
        }
        return new ResultSet(relAssocs.size(), relAssocs);
    }

    // ---

    @Override
    public Set<AssociationModel> getTopicAssociations(long topicId) {
        return fetchAssociations(fetchTopicNode(topicId));
    }

    @Override
    public Set<AssociationModel> getAssociationAssociations(long assocId) {
        return fetchAssociations(fetchAssociationNode(assocId));
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

    private AssociationModel buildAssociation(Node middleNode) {
        long id = middleNode.getId();
        String uri = middleNode.getProperty("uri");
        String typeUri = middleNode.getProperty("type_uri");;
        SimpleValue value = new SimpleValue(middleNode.getProperty("value"));
        List<RoleModel> roleModels = roleModels(middleNode);
        return new AssociationModel(id, uri, typeUri, roleModels.get(0), roleModels.get(1), value, null);
    }

    private List<RoleModel> roleModels(Node middleNode) {
        List<RoleModel> roleModels = new ArrayList();
        for (Relationship rel : middleNode.getRelationships(Direction.OUTGOING)) {
            Node node = rel.getEndNode();
            String roleTypeUri = rel.getType().name();
            RoleModel roleModel = NodeType.of(node).createRoleModel(node, roleTypeUri);
            roleModels.add(roleModel);
        }
        // sanity check
        if (roleModels.size() != 2) {
            throw new RuntimeException("Data inconsistency: mehta edge " + middleNode.getId() +
                " connects " + roleModels.size() + " mehta objects instead of 2");
        }
        //
        return roleModels;
    }

    // ---

    /**
     * Checks if a topic with the given URI exists in the database, and if so, throws an exception.
     * If an empty string is given no check is performed.
     *
     * @param   uri     The URI to check. Must not be null.
     */
    private void checkTopicUniqueness(String uri) {
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
            Node middleNode = rel.getStartNode();
            assocs.add(buildAssociation(middleNode));
        }
        return assocs;
    }

    // ---

    private void checkType(Node node, NodeType type) {
        if (NodeType.of(node) != type) {
            throw new IllegalArgumentException(type.error(node));
        }
    }

    // ---

    private void storeAndIndexUri(Node node, String uri) {
        node.setProperty("uri", uri);
        indexAttribute(node, uri, IndexMode.KEY, "uri");
    }

    private void storeAndIndexTypeUri(Node node, String typeUri) {
        node.setProperty("type_uri", typeUri);
        indexAttribute(node, typeUri, IndexMode.KEY, "type_uri");
    }

    private void storeAndIndexValue(Node node, Object value, IndexMode indexMode, String indexKey) {
        node.setProperty("value", value);
        indexAttribute(node, value, indexMode, indexKey);
    }

    // ---

    private void indexAttribute(Node node, Object value, IndexMode indexMode) {
        indexAttribute(node, value, indexMode, null);
    }

    private void indexAttribute(Node node, Object value, IndexMode indexMode, String indexKey) {
        if (indexMode == IndexMode.OFF) {
            return;
        } else if (indexMode == IndexMode.KEY) {
            exactNodeIndex.remove(node, indexKey);              // remove old
            exactNodeIndex.add(node, indexKey, value);          // index new
        } else if (indexMode == IndexMode.FULLTEXT) {
            fulltextNodeIndex.remove(node, KEY_FULLTEXT);       // remove old
            fulltextNodeIndex.add(node, KEY_FULLTEXT, value);   // index new
        } else if (indexMode == IndexMode.FULLTEXT_KEY) {
            fulltextNodeIndex.remove(node, indexKey);           // remove old
            fulltextNodeIndex.add(node, indexKey, value);       // index new
        } else {
            throw new RuntimeException("Index mode \"" + indexMode + "\" not implemented");
        }
    }

    //

    private String indexValue(long id, String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                  String othersTypeUri) {
        // ### TODO: wildcards
        String indexValue = id + "," + assocTypeUri + "," + myRoleTypeUri + "," + othersRoleTypeUri + "," +
            othersTypeUri;
        return indexValue;
    }
}
