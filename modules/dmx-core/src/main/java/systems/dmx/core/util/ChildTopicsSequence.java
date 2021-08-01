package systems.dmx.core.util;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;

import java.util.Iterator;



/**
 * Maintains the sequence of a parent topic's child topics.
 */
public class ChildTopicsSequence implements Iterable<RelatedTopic> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Topic parentTopic;
    private String childTypeUri;
    private String assocTypeUri;

    private CoreService dmx;
    private ModelFactory mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ChildTopicsSequence(Topic parentTopic, String childTypeUri, String assocTypeUri, CoreService dmx) {
        this.parentTopic = parentTopic;
        this.childTypeUri = childTypeUri;
        this.assocTypeUri = assocTypeUri;
        this.dmx = dmx;
        this.mf = dmx.getModelFactory();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterator<RelatedTopic> iterator() {

        return new Iterator() {

            private RelatedTopic topic = getFirst();

            @Override
            public boolean hasNext() {
                return topic != null;
            }

            @Override
            public RelatedTopic next() {
                RelatedTopic _topic = topic;
                topic = getSuccessor(topic);
                return _topic;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // ---

    /**
     * @param   predChildTopic    may be null.
     */
    public Topic insert(Topic childTopic, Topic predChildTopic) {
        long childTopicId = childTopic.getId();
        if (predChildTopic != null) {
            checkParent(predChildTopic);
            //
            long predChildTopicId = predChildTopic.getId();
            RelatedTopic succ = getSuccessor(predChildTopic);
            if (succ != null) {
                insertInBetween(childTopicId, predChildTopicId, succ);
            } else {
                createSequenceSegment(predChildTopicId, childTopicId);
            }
        } else {
            RelatedTopic first = getFirst();
            if (first != null) {
                insertAtBegin(childTopicId, first);
            } else {
                createSequenceStart(childTopicId);
            }
        }
        return childTopic;
    }

    /**
     * Removes the given child topic from the sequence.
     * <p>
     * Must be called <i>before</i> <code>childTopic</code> is removed from DB (if at all).
     *
     * @param   childTopic      not null.
     */
    public void remove(Topic childTopic) {
        checkParent(childTopic);
        //
        RelatedTopic pred = getPredecessor(childTopic);
        RelatedTopic succ = getSuccessor(childTopic);
        if (succ != null) {
            succ.getRelatingAssoc().delete();
        }
        if (pred != null) {
            pred.getRelatingAssoc().delete();
            if (succ != null) {
                createSequenceSegment(pred.getId(), succ.getId());
            }
        } else {
            getFirst().getRelatingAssoc().delete();
            if (succ != null) {
                createSequenceStart(succ.getId());
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void insertAtBegin(long childTopicId, RelatedTopic firstChildTopic) {
        firstChildTopic.getRelatingAssoc().delete();
        createSequenceStart(childTopicId);
        createSequenceSegment(childTopicId, firstChildTopic.getId());
    }

    private void insertInBetween(long childTopicId, long predChildTopicId, RelatedTopic succChildTopic) {
        succChildTopic.getRelatingAssoc().delete();
        createSequenceSegment(predChildTopicId, childTopicId);
        createSequenceSegment(childTopicId, succChildTopic.getId());
    }

    // ---

    private void createSequenceStart(long childTopicId) {
        dmx.createAssoc(mf.newAssocModel(ASSOCIATION,
            mf.newTopicPlayerModel(parentTopic.getId(), PARENT),
            mf.newTopicPlayerModel(childTopicId, SEQUENCE_START)
        ));
    }


    private void createSequenceSegment(long predTopicId, long succTopicId) {
        dmx.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newTopicPlayerModel(predTopicId, PREDECESSOR),
            mf.newTopicPlayerModel(succTopicId, SUCCESSOR)
        ));
    }

    // ---

    private RelatedTopic getPredecessor(Topic childTopic) {
        return childTopic.getRelatedTopic(SEQUENCE, SUCCESSOR, PREDECESSOR, childTypeUri);
    }

    private RelatedTopic getSuccessor(Topic childTopic) {
        return childTopic.getRelatedTopic(SEQUENCE, PREDECESSOR, SUCCESSOR, childTypeUri);
    }

    // ---

    private RelatedTopic getFirst() {
        return parentTopic.getRelatedTopic(ASSOCIATION, PARENT, SEQUENCE_START, childTypeUri);
    }

    // ---

    private void checkParent(Topic node) {
        if (!getParentNode(node).equals(parentTopic)) {
            throw new RuntimeException("Node " + node.getId() + " is not a child of node " + parentTopic.getId());
        }
    }

    private RelatedTopic getParentNode(Topic node) {
        RelatedTopic parentNode = getParentNodeIfExists(node);
        if (parentNode == null) {
            throw new RuntimeException("Node " + node.getId() + " has no parent node");
        }
        return parentNode;
    }

    // ### FIXME: ambiguity
    private RelatedTopic getParentNodeIfExists(Topic node) {
        return node.getRelatedTopic(assocTypeUri, CHILD, PARENT, parentTopic.getTypeUri());
    }
}
