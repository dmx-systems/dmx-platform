package de.deepamehta.core.model;

import de.deepamehta.core.Topic;
import java.util.List;



public class TopicReferenceModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicReferenceModel(long topicId) {
        super(topicId);
    }

    public TopicReferenceModel(String uri) {
        super(uri, (String) null);   // typeUri=null ### FIXME: OK?
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public boolean isReferenceById() {
        return getId() != -1;
    }

    public boolean isReferenceByUri() {
        return !getUri().equals("");
    }

    // ---

    /**
     * Checks weather this reference refers to the given topic.
     */
    public boolean isReferingTo(Topic topic) {
        if (isReferenceById()) {
            return getId() == topic.getId();
        } else if (isReferenceByUri()) {
            return getUri().equals(topic.getUri());
        } else {
            throw new RuntimeException("Invalid topic reference (" + this + ")");
        }
    }

    /**
     * Checks weather this reference refers to any of the given topics.
     */
    public boolean isReferingToAny(List<Topic> topics) {
        for (Topic topic : topics) {
            if (isReferingTo(topic)) {
                return true;
            }
        }
        return false;
    }
}
