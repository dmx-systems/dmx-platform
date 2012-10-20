package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;



public interface ObjectFactory {

    AssociationDefinition fetchAssociationDefinition(Association assoc);

    // ---

    Topic fetchWholeTopicType(Association assoc);

    Topic fetchPartTopicType(Association assoc);

    // ---

    RelatedTopicModel fetchWholeCardinality(long assocDefId);

    RelatedTopicModel fetchPartCardinality(long assocDefId);
}
