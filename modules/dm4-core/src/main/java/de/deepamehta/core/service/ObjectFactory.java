package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;



public interface ObjectFactory {

    AssociationDefinition fetchAssociationDefinition(Association assoc);

    // ---

    RelatedTopic fetchWholeCardinality(Association assoc);

    RelatedTopic fetchPartCardinality(Association assoc);
}
