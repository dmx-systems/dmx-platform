package de.deepamehta.core;



/**
 * A Topic-Association pair.
 */
public interface RelatedTopic extends Topic {

    Association getAssociation();

    void setAssociation(Association association);
}
