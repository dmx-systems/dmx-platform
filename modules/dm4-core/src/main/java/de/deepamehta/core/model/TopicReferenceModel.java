package de.deepamehta.core.model;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import java.util.List;



public interface TopicReferenceModel extends RelatedTopicModel {

    boolean isReferenceById();

    boolean isReferenceByUri();

    // ---

    /**
     * Checks weather this reference refers to the given topic.
     */
    boolean isReferingTo(Topic topic);

    /**
     * From the given topics finds the one this reference refers to.
     */
    RelatedTopic findReferencedTopic(List<RelatedTopic> topics);
}
