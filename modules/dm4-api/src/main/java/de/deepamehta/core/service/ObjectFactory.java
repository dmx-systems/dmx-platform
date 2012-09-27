package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;



public interface ObjectFactory {

    AssociationDefinition fetchAssociationDefinition(Association assoc);

    // ---

    public Topic fetchWholeTopicType(Association assoc);

    public Topic fetchPartTopicType(Association assoc);

    // ---

    RelatedTopic fetchWholeCardinality(Association assoc);

    RelatedTopic fetchPartCardinality(Association assoc);
}
