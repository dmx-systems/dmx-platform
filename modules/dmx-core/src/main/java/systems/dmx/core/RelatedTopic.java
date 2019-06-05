package systems.dmx.core;

import systems.dmx.core.model.RelatedTopicModel;



/**
 * A Topic-Assoc pair.
 */
public interface RelatedTopic extends RelatedObject, Topic {

    RelatedTopicModel getModel();
}
