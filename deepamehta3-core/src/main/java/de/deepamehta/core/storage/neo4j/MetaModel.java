package de.deepamehta.core.storage.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.Predicate;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.Traversal;



/**
 * A generic (DeepaMehta independant) meta model of <i>classes</i> and <i>properties</i> on-top of Neo4j.
 * <p>
 * Simplified version of the neo4j-meta-model component. Emulates its storage format.
 */
class MetaModel {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String META_OBJECT_KEY = "meta_model_name";
    private static final long NAMESPACE_NODE_ID = 2;    // FIXME: drop this

    private static enum RelType implements RelationshipType {
        META_CLASS, META_PROPERTY, META_HAS_PROPERTY, META_HAS_INSTANCE, META_NAMESPACE, REF_TO_META_SUBREF
    }

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private GraphDatabaseService graphDb;
    private IndexService index;

    private Node namespaceNode;

    // ---------------------------------------------------------------------------------------------------- Constructors

    // FIXME: drop isCleanInstall parameter and let the constructor examine weather DB initialization is required
    MetaModel(GraphDatabaseService graphDb, IndexService index, boolean isCleanInstall) {
        this.graphDb = graphDb;
        this.index = index;
        //
        if (isCleanInstall) {
            Node refNode = graphDb.getReferenceNode();
            Node metaNode = graphDb.createNode();
            namespaceNode = graphDb.createNode();
            if (namespaceNode.getId() != NAMESPACE_NODE_ID) {
                throw new RuntimeException("Namespace node not created with ID " +
                    NAMESPACE_NODE_ID + " but " + namespaceNode.getId());
            }
            refNode.createRelationshipTo(metaNode, RelType.REF_TO_META_SUBREF);
            metaNode.createRelationshipTo(namespaceNode, RelType.META_NAMESPACE);
        } else {
            // FIXME: find namespace node by traversal
            namespaceNode = graphDb.getNodeById(NAMESPACE_NODE_ID);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    Node getClass(String className) {
        Node classNode = lookup(className);
        if (classNode == null) {
            throw new RuntimeException("Topic type \"" + className + "\" is unknown");
        }
        return classNode;
    }

    Node getClass(Node instance) {
        Relationship relation = instance.getSingleRelationship(RelType.META_HAS_INSTANCE, Direction.INCOMING);
        if (relation == null) {
            throw new RuntimeException("Type of " + instance + " is unknown " +
                "(there is no incoming META_HAS_INSTANCE relationship)");
        }
        return relation.getOtherNode(instance);
    }

    Iterable<Node> getInstances(String className) {
        return traverse(getClass(className), RelType.META_HAS_INSTANCE, Direction.OUTGOING);
    }

    Node createInstance(String className) {
        Node instance = graphDb.createNode();
        getClass(className).createRelationshipTo(instance, RelType.META_HAS_INSTANCE);
        return instance;
    }

    Iterable<Node> getProperties(String className) {
        return traverse(getClass(className), RelType.META_HAS_PROPERTY, Direction.OUTGOING);
    }

    Iterable<Node> getAllClasses() {
        return traverse(namespaceNode, RelType.META_CLASS, Direction.OUTGOING);
    }

    Node createClass(String className) {
        Node classNode = lookup(className);
        if (classNode != null) {
            throw new RuntimeException("Topic type with URI \"" + className + "\" already exists");
        }
        return createObject(className, RelType.META_CLASS);
    }

    Node createProperty(String propName) {
        Node propNode = lookup(propName);
        if (propNode != null) {
            return propNode;
        }
        return createObject(propName, RelType.META_PROPERTY);
    }

    void addProperty(String className, Node propNode) {
        getClass(className).createRelationshipTo(propNode, RelType.META_HAS_PROPERTY);
    }

    void renameClass(String oldClassName, String className) {
        index(getClass(oldClassName), className);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Node lookup(String name) {
        return index.getSingleNode(META_OBJECT_KEY, name);
    }

    private Node createObject(String name, RelationshipType relType) {
        Node node = graphDb.createNode();
        index(node, name);
        namespaceNode.createRelationshipTo(node, relType);
        return node;
    }

    private void index(Node node, String name) {
        node.setProperty(META_OBJECT_KEY, name);
        index.removeIndex(node, META_OBJECT_KEY);
        index.index(node, META_OBJECT_KEY, name);
    }

    private Iterable<Node> traverse(Node node, RelationshipType relType, Direction direction) {
        TraversalDescription desc = Traversal.description();
        desc = desc.relationships(relType, direction);
        desc = desc.filter(new StartNodeFilter());
        return desc.traverse(node).nodes();
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    private class StartNodeFilter implements Predicate<Path> {
        @Override
        public boolean accept(Path path) {
            return path.length() > 0;
        }
    }
}
