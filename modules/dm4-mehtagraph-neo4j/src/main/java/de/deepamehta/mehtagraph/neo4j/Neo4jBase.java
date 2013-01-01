package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.core.storage.spi.MehtaObject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import static org.neo4j.helpers.collection.MapUtil.stringMap;
//
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



class Neo4jBase {

    // ------------------------------------------------------------------------------------------------------- Constants

    protected static final String KEY_IS_MEHTA_EDGE = "_is_mehta_edge";
    protected static final String KEY_FULLTEXT = "_fulltext";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected GraphDatabaseService neo4j = null;
    protected Neo4jRelationtypeCache relTypeCache;
    protected Index<Node> exactNodeIndex;
    protected Index<Node> fulltextNodeIndex;
    protected Index<Node> edgeIndex;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected Neo4jBase(String databasePath) {
        try {
            this.neo4j = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);
            this.relTypeCache = new Neo4jRelationtypeCache(neo4j);
            // indexes
            this.exactNodeIndex    = createExactIndex("exact-node-index");
            this.fulltextNodeIndex = createFulltextIndex("fulltext-node-index");
            this.associationIndex = createExactIndex("association-index");
        } catch (Exception e) {
            if (neo4j != null) {
                logger.info("Shutdown Neo4j");
                neo4j.shutdown();
            }
            throw new RuntimeException("Creating the Neo4j instance and indexes failed", e);
        }
    }

    protected Neo4jBase(Neo4jBase base) {
        this.neo4j = base.neo4j;
        this.relTypeCache = base.relTypeCache;
        this.exactNodeIndex = base.exactNodeIndex;
        this.fulltextNodeIndex = base.fulltextNodeIndex;
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected final Neo4jMehtaNode buildMehtaNode(Node node) {
        if (node == null) {
            throw new NullPointerException("Tried to build a MehtaNode from a null Node");
        }
        if (isAuxiliaryNode(node)) {
            throw new IllegalArgumentException("Reference error: ID " + node.getId() +
                " refers to a MehtaEdge when the caller expects a MehtaNode");
        }
        return new Neo4jMehtaNode(node, this);
    }

    protected final Neo4jMehtaEdge buildMehtaEdge(Node auxiliaryNode) {
        if (auxiliaryNode == null) {
            throw new NullPointerException("Tried to build a MehtaEdge from a null auxiliary Node");
        }
        if (!isAuxiliaryNode(auxiliaryNode)) {
            throw new IllegalArgumentException("Reference error: ID " + auxiliaryNode.getId() +
                " refers to a MehtaNode when the caller expects a MehtaEdge");
        }
        return new Neo4jMehtaEdge(auxiliaryNode, this);
    }

    protected final MehtaObject buildMehtaObject(Node node) {
        return isAuxiliaryNode(node) ? buildMehtaEdge(node) : buildMehtaNode(node);
    }

    // ---

    protected final RelationshipType getRelationshipType(String typeName) {
        return relTypeCache.get(typeName);
    }

    protected final boolean isAuxiliaryNode(Node node) {
        return (Boolean) node.getProperty(KEY_IS_MEHTA_EDGE, false);
    }



    // === Traversal ===

    /**
     * The created traversal description allows to find mehta nodes that
     * are connected to the start node/edge via the given role types.
     * <p>
     * Called from {@link Neo4jMehtaObject#getConnectedMehtaNodes}.
     *
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    protected final TraversalDescription traverseToMehtaNodes(String myRoleType, String othersRoleType) {
        return createTraversalDescription(new RoleTypeEvaluator(myRoleType, othersRoleType),
                                          new AuxiliaryEvaluator(false));
    }

    /**
     * The created traversal description allows to find mehta edges that
     * are connected to the start node/edge via the given role types.
     * <p>
     * Called from {@link Neo4jMehtaObject#getConnectedMehtaEdges}.
     *
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    protected final TraversalDescription traverseToMehtaEdges(String myRoleType, String othersRoleType) {
        return createTraversalDescription(new RoleTypeEvaluator(myRoleType, othersRoleType),
                                          new AuxiliaryEvaluator(true));
    }

    // ---

    /**
     * The created traversal description allows to find all mehta edges between
     * the the start node/edge and the given node.
     * <p>
     * Called from {@link Neo4jMehtaGraph#getMehtaEdges(long, long)}
     */
    protected final TraversalDescription traverseToMehtaNode(long nodeId) {
        return createTraversalDescription(new ConnectedNodeEvaluator(nodeId),
                                          new AuxiliaryEvaluator(false));
    }

    /**
     * The created traversal description allows to find all mehta edges between
     * the the start node/edge and the given node.
     * The created traversal description allows to find mehta nodes that
     * are connected to the start node/edge via the given role types.
     * <p>
     * Called from {@link Neo4jMehtaGraph#getMehtaEdges(long, long, String, String)}
     *
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    protected final TraversalDescription traverseToMehtaNode(long nodeId, String myRoleType, String othersRoleType) {
        return createTraversalDescription(new ConnectedNodeEvaluator(nodeId),
                                          new RoleTypeEvaluator(myRoleType, othersRoleType),
                                          new AuxiliaryEvaluator(false));
    }

    /**
     * The created traversal description allows to find all mehta edges between
     * the the start node/edge and the given edge.
     * The created traversal description allows to find mehta nodes that
     * are connected to the start node/edge via the given role types.
     * <p>
     * Called from {@link Neo4jMehtaGraph#getMehtaEdgesBetweenNodeAndEdge(long, long, String, String)}
     *
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    protected final TraversalDescription traverseToMehtaEdge(long edgeId, String myRoleType, String othersRoleType) {
        return createTraversalDescription(new ConnectedNodeEvaluator(edgeId),
                                          new RoleTypeEvaluator(myRoleType, othersRoleType),
                                          new AuxiliaryEvaluator(true));
    }

    // ---

    /**
     * Traverses from the specified start object and builds the traversal result.
     */
    protected abstract class TraveralResultBuilder {

        private Set result = new HashSet();

        protected TraveralResultBuilder(Neo4jMehtaObject startObject, TraversalDescription desc) {
            for (Path path : desc.traverse(startObject.getNode())) {
                // sanity check
                if (path.length() != 2) {
                    throw new RuntimeException("jri doesn't understand Neo4j traversal");
                }
                //
                Node connectedNode = path.endNode();
                Node auxiliaryNode = path.lastRelationship().getStartNode();
                result.add(buildResult(connectedNode, auxiliaryNode));
            }
        }

        abstract Object buildResult(Node connectedNode, Node auxiliaryNode);

        Set getResult() {
            return result;
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Indexing ===

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



    // === Traversal ===

    /**
     * Describes one mehta edge hop.
     */
    private TraversalDescription createTraversalDescription(Evaluator... evaluators) {
        TraversalDescription description = Traversal.description()
            .evaluator(new DirectionEvaluator())
            .uniqueness(Uniqueness.RELATIONSHIP_PATH);
        // Note 1: we need to traverse a node more than once. Consider this case: mehta node A
        // is connected with mehta node B via mehta edge C and A is connected to C as well.
        // (default uniqueness is not RELATIONSHIP_GLOBAL, but probably NODE_GLOBAL).
        //
        // Note 2: we also need to traverse a relationship more than once! Consider this case:
        // mehta node A is connected with itself (so, RELATIONSHIP_GLOBAL doesn't suit).
        //
        for (Evaluator evaluator : evaluators) {
            description = description.evaluator(evaluator);
        }
        //
        return description;
    }

    // ---

    private class DirectionEvaluator implements Evaluator {

        @Override
        public Evaluation evaluate(Path path) {
            boolean includes = false;
            boolean continues = false;
            //
            Relationship rel = path.lastRelationship();
            Node node = path.endNode();
            if (path.length() == 0) {
                continues = true;
            } else if (path.length() == 1) {
                continues = rel.getStartNode().getId() == node.getId();
            } else if (path.length() == 2) {
                includes = rel.getEndNode().getId() == node.getId();
            }
            //
            return Evaluation.of(includes, continues);
        }
    }

    private class RoleTypeEvaluator implements Evaluator {

        private RelationshipType myRoleType;
        private RelationshipType othersRoleType;

        private RoleTypeEvaluator(String myRoleType, String othersRoleType) {
            if (myRoleType != null) {
                this.myRoleType = getRelationshipType(myRoleType);
            }
            if (othersRoleType != null) {
                this.othersRoleType = getRelationshipType(othersRoleType);
            }
        }

        @Override
        public Evaluation evaluate(Path path) {
            boolean includes = true;
            boolean continues = true;
            //
            Relationship rel = path.lastRelationship();
            Node node = path.endNode();
            if (path.length() == 1) {
                continues = myRoleType == null || rel.isType(myRoleType);
            } else if (path.length() == 2) {
                includes = othersRoleType == null || rel.isType(othersRoleType);
            }
            //
            return Evaluation.of(includes, continues);
        }
    }

    private class ConnectedNodeEvaluator implements Evaluator {

        private long connectedNodeId;

        /**
         * @param   connectedNodeId     ID of a Neo4j node. This ID may represent a mehta node or a mehta edge.
         */
        private ConnectedNodeEvaluator(long connectedNodeId) {
            this.connectedNodeId = connectedNodeId;
        }

        @Override
        public Evaluation evaluate(Path path) {
            boolean includes = path.length() == 2 && path.endNode().getId() == connectedNodeId;
            boolean continues = true;
            return Evaluation.of(includes, continues);
        }
    }

    private class AuxiliaryEvaluator implements Evaluator {

        private boolean isAuxiliary;

        private AuxiliaryEvaluator(boolean isAuxiliary) {
            this.isAuxiliary = isAuxiliary;
        }

        @Override
        public Evaluation evaluate(Path path) {
            boolean includes = path.length() == 2 && isAuxiliaryNode(path.endNode()) == isAuxiliary;
            boolean continues = true;
            return Evaluation.of(includes, continues);
        }
    }
}
