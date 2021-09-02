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

    private long parentTopicId;
    private String childTypeUri;
    private String assocTypeUri;

    private AccessLayer al;
    private ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsSequence(long parentTopicId, String childTypeUri, String assocTypeUri, AccessLayer al) {
        this.parentTopicId = parentTopicId;
        this.childTypeUri = childTypeUri;
        this.assocTypeUri = assocTypeUri;
        this.al = al;
        this.mf = al.mf;
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

            private List assocIds = new ArrayList();

            @Override
            public boolean hasNext() {
                return assoc != null;
            }

            @Override
            public RelatedAssocModelImpl next() {
                RelatedAssocModelImpl _assoc = assoc;
                assoc = getSuccessorAssoc(assoc);
                //
                if (assocIds.contains(_assoc.id)) {
                    throw new RuntimeException("Cycle detected: assoc " + _assoc.id + " already in " + assocIds);
                }
                assocIds.add(_assoc.id);
                //
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
                parentTopicId + " failed (predTopicId=" + predTopicId + ")", e);
        }
    }

    void insert(AssocModelImpl assoc, long predTopicId) {
        try {
            _insert(assoc, predTopicId);
        } catch (Exception e) {
            throw new RuntimeException("Inserting association " + assoc.getId() + " into sequence of parent topic " +
                parentTopicId + " failed (predTopicId=" + predTopicId + ")", e);
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
                parentTopicId + " failed", e);
        }
    }

    RelatedTopicModelImpl remove(AssocModelImpl assoc) {
        try {
            _remove(assoc);
            return childTopic(assoc);
        } catch (Exception e) {
            throw new RuntimeException("Removing association " + assoc.getId() + " from sequence of parent topic " +
                parentTopicId + " failed", e);
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
        RelatedAssocModelImpl pred = getPredecessorAssoc(assoc.id);
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
            mf.newTopicPlayerModel(parentTopicId, DEFAULT),
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

    /* private RelatedAssocModelImpl getPredecessorAssoc(RelatedTopicModelImpl childTopic) {
        return getPredecessorAssoc(childTopic.getRelatingAssoc());
    } */

    /**
     * @return      possibly null
     */
    private RelatedAssocModelImpl getPredecessorAssoc(long assocId) {
        return al.getAssocRelatedAssoc(assocId, SEQUENCE, SUCCESSOR, PREDECESSOR, assocTypeUri);
    }

    // ---

    /**
     * @return  the association that connects the parent topic with its first child topic, or <code>null</code> if there
     *          is no child topic. The returned association's "relating association" is of type "Assoc" and
     *          connects the returned association (role type is "Sequence Start") with the parent topic.
     */
    private RelatedAssocModelImpl getFirstAssoc() {
        return al.getTopicRelatedAssoc(parentTopicId, SEQUENCE, DEFAULT, SEQUENCE_START, assocTypeUri);
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
                throw new RuntimeException("Ambiguity detected: assoc " + assoc.id + " has " + size + " successors: " +
                    succAssocs);
            }
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
        AssocModelImpl assoc = al.getAssocBetweenTopicAndTopic(assocTypeUri, parentTopicId, childTopicId, PARENT,
            CHILD);
        if (assoc == null) {
            throw new RuntimeException("Topic " + childTopicId + " is not a child of topic " + parentTopicId);
        }
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
}
