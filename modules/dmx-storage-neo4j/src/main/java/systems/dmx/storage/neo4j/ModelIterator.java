package systems.dmx.storage.neo4j;

import systems.dmx.core.impl.AssocModelImpl;
import systems.dmx.core.impl.TopicModelImpl;
import systems.dmx.core.model.DMXObjectModel;

import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.Iterator;
import java.util.NoSuchElementException;



class TopicModelIterable extends ModelIterable<TopicModelImpl> {

    TopicModelIterable(Neo4jStorage storage) {
        super(storage, NodeType.TOPIC);
    }

    @Override
    TopicModelImpl buildModel(Node node) {
        return storage.buildTopic(node);
    }
}



class AssocModelIterable extends ModelIterable<AssocModelImpl> {

    AssocModelIterable(Neo4jStorage storage) {
        super(storage, NodeType.ASSOC);
    }

    @Override
    AssocModelImpl buildModel(Node node) {
        return storage.buildAssoc(node);
    }
}



abstract class ModelIterable<M extends DMXObjectModel> implements Iterable<M> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected Neo4jStorage storage;

    private NodeType nodeType;      // the node type we're looking for
    private Iterable<Node> nodes;   // all nodes in the DB

    // ---------------------------------------------------------------------------------------------------- Constructors

    ModelIterable(Neo4jStorage storage, NodeType nodeType) {
        this.storage = storage;
        this.nodeType = nodeType;
        this.nodes = GlobalGraphOperations.at(storage.neo4j).getAllNodes();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterator<M> iterator() {
        return new ModelIterator();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract M buildModel(Node node);

    // ---------------------------------------------------------------------------------------------------- Nested Class

    private class ModelIterator implements Iterator<M> {

        private Iterator<Node> i = nodes.iterator();
        private Node next;      // next matching node; updated by fetchNext()

        @Override
        public boolean hasNext() {
            fetchNext();
            return next != null;
        }

        @Override
        public M next() {
            if (next == null) {
                throw new NoSuchElementException("next() called when there is no next node");
            }
            return buildModel(next);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("removal is not supported");
        }

        private void fetchNext() {
            next = null;
            while (i.hasNext() && next == null) {
                Node node = i.next();
                if (nodeType.isTypeOf(node)) {
                    next = node;
                }
            }
        }
    }
}
