package systems.dmx.core.model;

import java.util.List;



public interface TopicReferenceModel extends RelatedTopicModel {

    boolean isReferenceById();

    boolean isReferenceByUri();

    /**
     * Returns true if this reference refers to nothing.
     */
    boolean isEmptyRef();

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
