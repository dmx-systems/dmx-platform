package de.deepamehta.core;

import de.deepamehta.core.model.RelatedTopicModel;



/**
 * A Topic-Association pair.
 */
public interface RelatedTopic extends Topic {

    Association getRelatingAssociation();   // ### TODO: rename to getAssociation()?

    RelatedTopicModel getModel();
}
