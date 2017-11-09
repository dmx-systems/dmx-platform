package de.deepamehta.core.model;

import java.util.List;



public interface TopicReferenceModel extends RelatedTopicModel {

    boolean isReferenceById();

    boolean isReferenceByUri();

    // ---

    /**
     * Checks weather this reference refers to the given topic.
     */
    boolean isReferingTo(TopicModel topic);

    /**
     * From the given topics finds the one this reference refers to.
     */
    RelatedTopicModel findReferencedTopic(List<? extends RelatedTopicModel> topics);
}
