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
     * @param   beforeChildTopic    may be null.
     */
    public Topic add(Topic childTopic, Topic beforeChildTopic) {
        long childTopicId = childTopic.getId();
        if (beforeChildTopic != null) {
            checkChildTopic(beforeChildTopic);
            //
            RelatedTopic pred = getPredecessor(beforeChildTopic);
            if (pred != null) {
                insertInBetween(childTopicId, pred, beforeChildTopic.getId());
            } else {
                insertAtBegin(childTopicId, getFirst());
            }
        } else {
            RelatedTopic last = getLast();
            if (last != null) {
                createSequenceSegment(last.getId(), childTopicId);
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
        checkChildTopic(childTopic);
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

    private void insertInBetween(long childTopicId, RelatedTopic predChildTopic, long succChildTopicId) {
        predChildTopic.getRelatingAssoc().delete();
        createSequenceSegment(predChildTopic.getId(), childTopicId);
        createSequenceSegment(childTopicId, succChildTopicId);
    }

    // ---

    private void createSequenceStart(long childTopicId) {
        dmx.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newTopicPlayerModel(parentTopic.getId(), DEFAULT),
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
        return parentTopic.getRelatedTopic(SEQUENCE, DEFAULT, SEQUENCE_START, childTypeUri);
    }

    private RelatedTopic getLast() {
        RelatedTopic childTopic = null;
        Iterator<RelatedTopic> i = iterator();
        while (i.hasNext()) {
            childTopic = i.next();
        }
        return childTopic;
    }

    // ---

    private void checkChildTopic(Topic topic) {
        if (!getParentTopic(topic).equals(parentTopic)) {
            throw new RuntimeException("Topic " + topic.getId() + " is not a child of topic " + parentTopic.getId());
        }
    }

    private RelatedTopic getParentTopic(Topic topic) {
        RelatedTopic parentTopic = getParentTopicIfExists(topic);
        if (parentTopic == null) {
            throw new RuntimeException("Topic " + topic.getId() + " has no parent topic");
        }
        return parentTopic;
    }

    // ### FIXME: ambiguity
    private RelatedTopic getParentTopicIfExists(Topic topic) {
        return topic.getRelatedTopic(assocTypeUri, CHILD, PARENT, parentTopic.getTypeUri());
    }
}
