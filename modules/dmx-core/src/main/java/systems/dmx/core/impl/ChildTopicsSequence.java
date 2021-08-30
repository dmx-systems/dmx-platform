package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * Maintains the sequence of a parent topic's child topics.
 * <p>
 * ### Status: experimental. Partly functional. API will change.
 */
class ChildTopicsSequence implements Iterable<RelatedAssocModelImpl> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModelImpl parentTopic;
    private String childTypeUri;
    private String assocTypeUri;

    private AccessLayer al;
    private ModelFactoryImpl mf;
    private CycleDetection detection;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsSequence(TopicModelImpl parentTopic, String childTypeUri, String assocTypeUri, AccessLayer al) {
        this.parentTopic = parentTopic;
        this.childTypeUri = childTypeUri;
        this.assocTypeUri = assocTypeUri;
        this.al = al;
        this.mf = al.mf;
        // this.detection = detection;      // TODO
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @return  an iterator of the parent topic's child topics (<code>RelatedTopic</code>s).
     *          Their "relating association" is the respective parent connection. ### FIXDOC
     */
    @Override
    public Iterator<RelatedAssocModelImpl> iterator() {

        return new Iterator() {

            private RelatedAssocModelImpl assoc = getFirstAssoc();

            @Override
            public boolean hasNext() {
                return assoc != null;
            }

            @Override
            public RelatedAssocModelImpl next() {
                RelatedAssocModelImpl _assoc = assoc;
                assoc = getSuccessorAssoc(assoc);
                return _assoc;
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
    RelatedTopicModelImpl insert(long childTopicId, long predTopicId) {
        try {
            RelatedTopicModelImpl childTopic = getChildTopic(childTopicId);
            _insert(childTopic.getRelatingAssoc(), predTopicId);
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Inserting topic " + childTopicId + " into sequence of parent topic " +
                parentTopic.getId() + " failed (predTopicId=" + predTopicId + ")", e);
        }
    }

    void insert(AssocModelImpl assoc, long predTopicId) {
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
    RelatedTopicModelImpl remove(long childTopicId) {
        try {
            RelatedTopicModelImpl childTopic = getChildTopic(childTopicId);
            _remove(childTopic.getRelatingAssoc());
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Removing topic " + childTopicId + " from sequence of parent topic " +
                parentTopic.getId() + " failed", e);
        }
    }

    RelatedTopicModelImpl remove(AssocModelImpl assoc) {
        try {
            _remove(assoc);
            return childTopic(assoc);
        } catch (Exception e) {
            throw new RuntimeException("Removing association " + assoc.getId() + " from sequence of parent topic " +
                parentTopic.getId() + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void _insert(AssocModelImpl assoc, long predTopicId) {
        long assocId = assoc.getId();
        if (predTopicId != -1) {
            AssocModelImpl predAssoc = getChildTopic(predTopicId).getRelatingAssoc();
            RelatedAssocModelImpl succAssoc = getSuccessorAssoc(predAssoc);
            if (succAssoc != null) {
                insertInBetween(assocId, predAssoc, succAssoc);
            } else {
                createSequenceSegment(predAssoc.getId(), assocId);
            }
        } else {
            RelatedAssocModelImpl first = getFirstAssoc();
            if (first != null) {
                insertAtBegin(assocId, first);
            } else {
                createSequenceStart(assocId);
            }
        }
    }

    private void _remove(AssocModelImpl assoc) {
        RelatedAssocModelImpl pred = getPredecessorAssoc(assoc);
        RelatedAssocModelImpl succ = getSuccessorAssoc(assoc);
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
    private void insertAtBegin(long assocId, RelatedAssocModelImpl firstChildAssoc) {
        firstChildAssoc.getRelatingAssoc().delete();
        createSequenceStart(assocId);
        createSequenceSegment(assocId, firstChildAssoc.getId());
    }

    private void insertInBetween(long assocId, AssocModelImpl predAssoc, RelatedAssocModelImpl succAssoc) {
        succAssoc.getRelatingAssoc().delete();
        createSequenceSegment(predAssoc.getId(), assocId);
        createSequenceSegment(assocId, succAssoc.getId());
    }

    // ---

    private void createSequenceStart(long assocId) {
        al.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newTopicPlayerModel(parentTopic.getId(), DEFAULT),
            mf.newAssocPlayerModel(assocId, SEQUENCE_START)
        ));
    }

    private void createSequenceSegment(long predAssocId, long succAssocId) {
        al.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newAssocPlayerModel(predAssocId, PREDECESSOR),
            mf.newAssocPlayerModel(succAssocId, SUCCESSOR)
        ));
    }

    // ---

    private RelatedAssocModelImpl getPredecessorAssoc(RelatedTopicModelImpl childTopic) {
        return getPredecessorAssoc(childTopic.getRelatingAssoc());
    }

    /**
     * @return      possibly null
     */
    private RelatedAssocModelImpl getPredecessorAssoc(AssocModelImpl assoc) {
        RelatedAssocModelImpl predAssoc = al.getAssocRelatedAssoc(assoc.getId(), SEQUENCE, SUCCESSOR, PREDECESSOR,
            assocTypeUri);
        if (predAssoc != null) {
            checkAssoc(predAssoc);
        }
        return predAssoc;
    }

    // ---

    /**
     * @return  the association that connects the parent topic with its first child topic, or <code>null</code> if there
     *          is no child topic. The returned association's "relating association" is of type "Assoc" and
     *          connects the returned association (role type is "Sequence Start") with the parent topic.
     */
    private RelatedAssocModelImpl getFirstAssoc() {
        RelatedAssocModelImpl assoc = al.getTopicRelatedAssoc(parentTopic.getId(), SEQUENCE, DEFAULT, SEQUENCE_START,
            assocTypeUri);
        if (assoc != null) {
            checkAssoc(assoc);
        }
        return assoc;
    }

    /**
     * @return  the parent topic's first child topic, or <code>null</code> if there is no child topic.
     *          The returned topic's "relating association" is its parent connection.
     */
    private RelatedTopicModelImpl getFirstTopic() {
        AssocModelImpl assoc = getFirstAssoc();
        return assoc != null ? childTopic(assoc) : null;
    }

    /**
     * @param   childTopic      its "relating association" is expected to be its parent connection.
     *
     * @return  the given child topic's successor child topic, or <code>null</code> if there is no successor.
     *          The returned topic's "relating association" is its parent connection.
     */
    private RelatedTopicModelImpl getSuccessorTopic(RelatedTopicModelImpl childTopic) {
        AssocModelImpl assoc = getSuccessorAssoc(childTopic);
        return assoc != null ? childTopic(assoc) : null;
    }

    // ---

    /**
     * Convenience.
     */
    private RelatedAssocModelImpl getSuccessorAssoc(RelatedTopicModelImpl childTopic) {
        return getSuccessorAssoc(childTopic.getRelatingAssoc());
    }

    /**
     * @param   assoc   expected to connect the parent topic with a child topic.
     *
     * @return  the given association's successor association, or <code>null</code> if there is no successor.
     *          The returned association's "relating association" is of type "Sequence".
     */
    private RelatedAssocModelImpl getSuccessorAssoc(AssocModelImpl assoc) {
        List<RelatedAssocModelImpl> succAssocs = al.getAssocRelatedAssocs(assoc.id, SEQUENCE, PREDECESSOR, SUCCESSOR,
            assocTypeUri);
        RelatedAssocModelImpl succAssoc = null;
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
     * Returns the <code>RelatedTopic</code> representation of the specified child topic.
     *
     * @param   childTopicId    ID of the child topic to return.
     *
     * @return  a <code>RelatedTopic</code> that represents the specified child topic.
     *          Its "relating association" is the one that connects this sequence's parent topic with the child topic.
     *
     * @throws  RuntimeException    if the specified ID is not one of this sequence's child topics.
     */
    private RelatedTopicModelImpl getChildTopic(long childTopicId) {
        AssocModelImpl assoc = al.getAssocBetweenTopicAndTopic(assocTypeUri, parentTopic.getId(), childTopicId, PARENT,
            CHILD);
        if (assoc == null) {
            throw new RuntimeException("Topic " + childTopicId + " is not a child of topic " + parentTopic.getId());
        }
        checkAssoc(assoc);
        RelatedTopicModelImpl childTopic = childTopic(assoc);
        checkTopic(childTopic);
        return childTopic;
    }

    private RelatedTopicModelImpl childTopic(AssocModelImpl assoc) {
        return (RelatedTopicModelImpl) assoc.getDMXObjectByRole(CHILD);
    }

    private long assocId(RelatedTopicModelImpl childTopic) {
        return childTopic.getRelatingAssoc().getId();
    }

    private void checkTopic(TopicModelImpl topic) {
        String typeUri = topic.getTypeUri();
        if (!typeUri.equals(childTypeUri)) {
            throw new RuntimeException("Topic " + topic.getId() + " is of type \"" + typeUri +
                "\" but expected is \"" + childTypeUri + "\"");
        }
    }

    private void checkAssoc(AssocModelImpl assoc) {
        String typeUri = assoc.getTypeUri();
        if (!typeUri.equals(assocTypeUri)) {
            throw new RuntimeException("Assoc " + assoc.getId() + " is of type \"" + typeUri +
                "\" but expected is \"" + assocTypeUri + "\"");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private static class CycleDetection {

        // child node sequence cycle detection
        TopicModelImpl parentNode;
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

        void startSequence(TopicModelImpl parentNode) {
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

        void reportAmbiguity(TopicModelImpl parentTopic, AssocModelImpl assoc, List<RelatedAssocModelImpl> succAssocs) {
            logger.warning("### Ambiguity detected in child node sequence of parent node " + parentNode.getId() +
                ". Assoc " + assoc.getId() + " has " + succAssocs.size() + " successors: " + succAssocs);
            sequenceAmbiguities++;
        }
    }
}
