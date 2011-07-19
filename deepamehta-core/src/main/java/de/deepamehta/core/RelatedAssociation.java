package de.deepamehta.core;



/**
 * An Association-Association pair.
 */
public interface RelatedAssociation extends Association {

    Association getRelatingAssociation();
}
