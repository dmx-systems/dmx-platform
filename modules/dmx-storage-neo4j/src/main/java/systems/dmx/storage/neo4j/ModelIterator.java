package systems.dmx.storage.neo4j;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.TopicModel;

import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.Iterator;
import java.util.NoSuchElementException;



class TopicModelIterable extends ModelIterable<TopicModel> {

    TopicModelIterable(Neo4jStorage storage) {
        super(storage, NodeType.TOPIC);
    }

    @Override
    TopicModel buildModel(Node node) {
        return storage.buildTopic(node);
    }
}



class AssocModelIterable extends ModelIterable<AssocModel> {

    AssocModelIterable(Neo4jStorage storage) {
        super(storage, NodeType.ASSOC);
    }

    @Override
    AssocModel buildModel(Node node) {
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

        Iterator<Node> i = nodes.iterator();
        Node next = null;       // next matching node; updated by fetchNext()

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
