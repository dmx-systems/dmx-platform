package systems.dmx.core.util;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedAssoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



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
    private CycleDetection detection;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ChildTopicsSequence(Topic parentTopic, String childTypeUri, String assocTypeUri, CoreService dmx) {
        this.parentTopic = parentTopic;
        this.childTypeUri = childTypeUri;
        this.assocTypeUri = assocTypeUri;
        this.dmx = dmx;
        this.mf = dmx.getModelFactory();
        // this.detection = detection;      // TODO
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @return  an iterator of the parent topic's child topics (<code>RelatedTopic</code>s).
     *          Their "relating association" is the respective parent connection.
     */
    @Override
    public Iterator<RelatedTopic> iterator() {

        return new Iterator() {

            private RelatedTopic topic = getFirstTopic();

            @Override
            public boolean hasNext() {
                return topic != null;
            }

            @Override
            public RelatedTopic next() {
                RelatedTopic _topic = topic;
                topic = getSuccessorTopic(topic);
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
     * Convenience.
     *
     * @param   predTopicId     may be -1
     */
    public Topic insert(long childTopicId, long predTopicId) {
        try {
            RelatedTopic childTopic = getChildTopic(childTopicId);
            _insert(childTopic.getRelatingAssoc(), predTopicId);
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Inserting topic " + childTopicId + " into sequence of parent topic " +
                parentTopic.getId() + " failed (predTopicId=" + predTopicId + ")", e);
        }
    }

    public void insert(Assoc assoc, long predTopicId) {
        try {
            _insert(assoc, predTopicId);
        } catch (Exception e) {
            throw new RuntimeException("Inserting association " + assoc.getId() + " into sequence of parent topic " +
                parentTopic.getId() + " failed (predTopicId=" + predTopicId + ")", e);
        }
    }

    // ---

    /**
     * Convenience.
     * <p>
     * Removes the given child topic (actually the association that connects it to parent) from this sequence.
     * The child topic itself is not deleted, nor the association that connects it to parent.
     * <p>
     * Must be called <i>before</i> the child topic (or the association that connects it to parent) is deleted
     * (if at all).
     *
     * @param   childTopicId    the child topic to remove
     *
     * @return  the <code>RelatedTopic</code> representation of the child topic.
     */
    public RelatedTopic remove(long childTopicId) {
        try {
            RelatedTopic childTopic = getChildTopic(childTopicId);
            _remove(childTopic.getRelatingAssoc());
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Removing topic " + childTopicId + " from sequence of parent topic " +
                parentTopic.getId() + " failed", e);
        }
    }

    public RelatedTopic remove(Assoc assoc) {
        try {
            _remove(assoc);
            return childTopic(assoc);
        } catch (Exception e) {
            throw new RuntimeException("Removing association " + assoc.getId() + " from sequence of parent topic " +
                parentTopic.getId() + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void _insert(Assoc assoc, long predTopicId) {
        long assocId = assoc.getId();
        if (predTopicId != -1) {
            Assoc predAssoc = getChildTopic(predTopicId).getRelatingAssoc();
            RelatedAssoc succAssoc = getSuccessorAssoc(predAssoc);
            if (succAssoc != null) {
                insertInBetween(assocId, predAssoc, succAssoc);
            } else {
                createSequenceSegment(predAssoc.getId(), assocId);
            }
        } else {
            RelatedAssoc first = getFirstAssoc();
            if (first != null) {
                insertAtBegin(assocId, first);
            } else {
                createSequenceStart(assocId);
            }
        }
    }

    private void _remove(Assoc assoc) {
        RelatedAssoc pred = getPredecessorAssoc(assoc);
        RelatedAssoc succ = getSuccessorAssoc(assoc);
        if (succ != null) {
            succ.getRelatingAssoc().delete();
        }
        if (pred != null) {
            pred.getRelatingAssoc().delete();
            if (succ != null) {
                createSequenceSegment(pred.getId(), succ.getId());
            }
        } else {
            getFirstAssoc().getRelatingAssoc().delete();
            if (succ != null) {
                createSequenceStart(succ.getId());
            }
        }
    }

    // ---

    /**
     * @param   firstChildAssoc     the association that connects the parent topic with its first child topic.
     *                              This association's "relating association" is its parent connection.
     */
    private void insertAtBegin(long assocId, RelatedAssoc firstChildAssoc) {
        firstChildAssoc.getRelatingAssoc().delete();
        createSequenceStart(assocId);
        createSequenceSegment(assocId, firstChildAssoc.getId());
    }

    private void insertInBetween(long assocId, Assoc predAssoc, RelatedAssoc succAssoc) {
        succAssoc.getRelatingAssoc().delete();
        createSequenceSegment(predAssoc.getId(), assocId);
        createSequenceSegment(assocId, succAssoc.getId());
    }

    // ---

    private void createSequenceStart(long assocId) {
        dmx.createAssoc(mf.newAssocModel(ASSOCIATION,
            mf.newTopicPlayerModel(parentTopic.getId(), PARENT),
            mf.newAssocPlayerModel(assocId, SEQUENCE_START)
        ));
    }

    private void createSequenceSegment(long predAssocId, long succAssocId) {
        dmx.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newAssocPlayerModel(predAssocId, PREDECESSOR),
            mf.newAssocPlayerModel(succAssocId, SUCCESSOR)
        ));
    }

    // ---

    private RelatedAssoc getPredecessorAssoc(RelatedTopic childTopic) {
        return getPredecessorAssoc(childTopic.getRelatingAssoc());
    }

    /**
     * @return      possibly null
     */
    private RelatedAssoc getPredecessorAssoc(Assoc assoc) {
        RelatedAssoc predAssoc = assoc.getRelatedAssoc(SEQUENCE, SUCCESSOR, PREDECESSOR,
            null);    // othersAssocTypeUri=null
        if (predAssoc != null) {
            checkAssoc(predAssoc);
        }
        return predAssoc;
    }

    // ---

    /**
     * Convenience.
     */
    private RelatedAssoc getSuccessorAssoc(RelatedTopic childTopic) {
        return getSuccessorAssoc(childTopic.getRelatingAssoc());
    }

    /**
     * @param   assoc   expected to connect the parent topic with a child topic.
     *
     * @return  the given association's successor association, or <code>null</code> if there is no successor.
     *          The returned association's "relating association" is of type "Sequence".
     */
    private RelatedAssoc getSuccessorAssoc(Assoc assoc) {
        List<RelatedAssoc> succAssocs = assoc.getRelatedAssocs(SEQUENCE, PREDECESSOR, SUCCESSOR,
            null);    // othersAssocTypeUri=null
        RelatedAssoc succAssoc = null;
        int size = succAssocs.size();
        if (size >= 1) {
            succAssoc = succAssocs.get(0);
            // ambiguity detection
            if (size > 1) {
                detection.reportAmbiguity(parentTopic, assoc, succAssocs);
            }
        }
        if (succAssoc != null) {
            checkAssoc(succAssoc);
        }
        return succAssoc;
    }

    // ---

    /**
     * @return  the association that connects the parent topic with its first child topic, or <code>null</code> if there
     *          is no child topic. The returned association's "relating association" is of type "Assoc" and
     *          connects the returned association (role type is "Sequence Start") with the parent topic.
     */
    private RelatedAssoc getFirstAssoc() {
        RelatedAssoc assoc = parentTopic.getRelatedAssoc(ASSOCIATION, PARENT, SEQUENCE_START,
            null);  // othersAssocTypeUri=null
        if (assoc != null) {
            checkAssoc(assoc);
        }
        return assoc;
    }

    /**
     * @return  the parent topic's first child topic, or <code>null</code> if there is no child topic.
     *          The returned topic's "relating association" is its parent connection.
     */
    private RelatedTopic getFirstTopic() {
        Assoc assoc = getFirstAssoc();
        return assoc != null ? childTopic(assoc) : null;
    }

    /**
     * @param   childTopic      its "relating association" is expected to be its parent connection.
     *
     * @return  the given child topic's successor child topic, or <code>null</code> if there is no successor.
     *          The returned topic's "relating association" is its parent connection.
     */
    private RelatedTopic getSuccessorTopic(RelatedTopic childTopic) {
        Assoc assoc = getSuccessorAssoc(childTopic);
        return assoc != null ? childTopic(assoc) : null;
    }

    // ---

    /**
     * Returns the <code>RelatedTopic</code> representation of the specified child topic.
     *
     * @param   childTopicId    ID of the child topic to return.
     *
     * @return  a <code>RelatedTopic</code> that represents the specified child topic.
     *          Its "relating association" is the one that connects this sequence's parent topic with the child topic.
     *
     * @throws  RuntimeException    if the specified ID is not one of this sequence's child topics.
     */
    private RelatedTopic getChildTopic(long childTopicId) {
        Assoc assoc = dmx.getAssocBetweenTopicAndTopic(assocTypeUri, parentTopic.getId(), childTopicId, PARENT, CHILD);
        if (assoc == null) {
            throw new RuntimeException("Node " + childTopicId + " is not a child of node " + parentTopic.getId());
        }
        checkAssoc(assoc);
        RelatedTopic childTopic = childTopic(assoc);
        checkTopic(childTopic);
        return childTopic;
    }

    private RelatedTopic childTopic(Assoc assoc) {
        return assoc.getDMXObjectByRole(CHILD);
    }

    private long assocId(RelatedTopic childTopic) {
        return childTopic.getRelatingAssoc().getId();
    }

    private void checkTopic(Topic topic) {
        String typeUri = topic.getTypeUri();
        if (!typeUri.equals(childTypeUri)) {
            throw new RuntimeException("Topic " + topic.getId() + " is of type \"" + typeUri +
                "\" but expected is \"" + childTypeUri + "\"");
        }
    }

    private void checkAssoc(Assoc assoc) {
        String typeUri = assoc.getTypeUri();
        if (!assocTypeUri.equals(typeUri)) {
            throw new RuntimeException("Assoc " + assoc.getId() + " is of type \"" + typeUri +
                "\" but expected is \"" + assocTypeUri + "\"");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private static class CycleDetection {

        // child node sequence cycle detection
        Topic parentNode;
        List<Long> childNodeIds;
        int sequenceCycles = 0;

        // child node sequence ambiguity detection
        int sequenceAmbiguities = 0;

        private Logger logger = Logger.getLogger(getClass().getName());

        // ------------------------------------------------------------------------------------------------ Constructors

        private CycleDetection() {
        }

        // ------------------------------------------------------------------------------------- Package Private Methods

        // child node sequence cycle detection

        void startSequence(Topic parentNode) {
            this.parentNode = parentNode;
            this.childNodeIds = new ArrayList();
        }

        boolean checkSequence(long nodeId) {
            boolean cycle = childNodeIds.contains(nodeId);
            if (!cycle) {
                childNodeIds.add(nodeId);
            } else {
                logger.warning("### Cycle detected in child node sequence of parent node " + parentNode.getId() +
                    ". Offending node ID: " + nodeId + ". Nodes in sequence so far: " + childNodeIds.size());
                sequenceCycles++;
            }
            return !cycle;
        }

        // child node sequence ambiguity detection

        void reportAmbiguity(Topic parentTopic, Assoc assoc, List<RelatedAssoc> succAssocs) {
            logger.warning("### Ambiguity detected in child node sequence of parent node " + parentNode.getId() +
                ". Assoc " + assoc.getId() + " has " + succAssocs.size() + " successors: " + succAssocs);
            sequenceAmbiguities++;
        }
    }
}
