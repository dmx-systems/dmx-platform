package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TopicModel;
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



    // === Mehta Nodes ===

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
        Node node = neo4j.getNodeById(id);
        checkTopicRef(node);
        return buildTopic(node);
    }

    @Override
    public TopicModel getMehtaNode(String key, Object value) {
        Node node = exactNodeIndex.get(key, value).getSingle();
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
    public List<TopicModel> getMehtaNodes(String key, Object value) {
        List<TopicModel> nodes = new ArrayList();
        for (Node node : exactNodeIndex.query(key, value)) {
            nodes.add(buildTopic(node));
        }
        return nodes;
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
        List<TopicModel> nodes = new ArrayList();
        for (Node node : fulltextNodeIndex.query(key, value)) {
            nodes.add(buildTopic(node));
        }
        return nodes;
    }



    // === Mehta Edges ===

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



    // === Mehta Objects ===

    @Override
    public MehtaObject getMehtaObject(long id) {
        return buildMehtaObject(neo4j.getNodeById(id));
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

    private void checkTopicRef(Node node) {
        if (!node.getProperty("node_type").equals("topic")) {
            throw new IllegalArgumentException("Reference error: ID " + node.getId() +
                " refers to an Association when the caller expects a Topic");
        }
    }

    private void checkAssociationRef(Node node) {
        if (!node.getProperty("node_type").equals("assoc")) {
            throw new IllegalArgumentException("Reference error: ID " + node.getId() +
                " refers to a Topic when the caller expects an Association");
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

    // ---

    public void indexAttribute(Node node, Object value, IndexMode indexMode) {
        indexAttribute(node, value, indexMode, null);
    }

    public void indexAttribute(Node node, Object value, IndexMode indexMode, String indexKey) {
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
}
