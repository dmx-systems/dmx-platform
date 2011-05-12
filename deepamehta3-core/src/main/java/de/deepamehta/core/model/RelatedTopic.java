package de.deepamehta.core.model;



/**
 * A Topic-Association pair.
 */
public interface RelatedTopic extends Topic {

    Association getAssociation();

    void setAssociation(Association association);
}
