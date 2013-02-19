package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RelatedTopicModel;



public interface TypeStorage {

    AssociationDefinitionModel fetchAssociationDefinition(Association assoc);

    // ---

    Topic fetchParentType(Association assoc);

    Topic fetchChildType(Association assoc);
}
