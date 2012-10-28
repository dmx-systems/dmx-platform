package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RelatedTopicModel;



public interface ObjectFactory {

    AssociationDefinitionModel fetchAssociationDefinition(Association assoc);

    // ---

    Topic fetchWholeTopicType(Association assoc);

    Topic fetchPartTopicType(Association assoc);
}
