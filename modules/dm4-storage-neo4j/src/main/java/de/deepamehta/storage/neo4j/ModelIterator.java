package de.deepamehta.storage.neo4j;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.TopicModel;

import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.Iterator;
import java.util.NoSuchElementException;



class TopicModelIterator extends ModelIterator<TopicModel> {

    TopicModelIterator(Neo4jStorage storage) {
        super(storage, NodeType.TOPIC);
    }

    @Override
    TopicModel buildModel(Node node) {
        return storage.buildTopic(node);
    }
}



class AssociationModelIterator extends ModelIterator<AssociationModel> {

    AssociationModelIterator(Neo4jStorage storage) {
        super(storage, NodeType.ASSOC);
    }

    @Override
    AssociationModel buildModel(Node node) {
        return storage.buildAssociation(node);
    }
}



abstract class ModelIterator<E extends DeepaMehtaObjectModel> implements Iterator<E> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected Neo4jStorage storage;

    private NodeType nodeType;
    private Iterator<Node> nodes;
    private Node nextNode;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ModelIterator(Neo4jStorage storage, NodeType nodeType) {
        this.storage = storage;
        this.nodeType = nodeType;
        this.nodes = GlobalGraphOperations.at(storage.neo4j).getAllNodes().iterator();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public boolean hasNext() {
        nextNode = fetchNextNode();
        return nextNode != null;
    }

    @Override
    public E next() {
        if (nextNode == null) {
            throw new NoSuchElementException("there is no next node");
        }
        //
        return buildModel(nextNode);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("removal is not supported");
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract E buildModel(Node node);

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Node fetchNextNode() {
        while (nodes.hasNext()) {
            Node node = nodes.next();
            if (nodeType.isTypeOf(node)) {
                return node;
            }
        }
        return null;
    }
}
