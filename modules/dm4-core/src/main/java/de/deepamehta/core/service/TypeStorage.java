package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.AssociationDefinitionModel;



public interface TypeStorage {

    /**
     * @param   assoc   an association representing an association definition
     *
     * @return  the parent type topic.
     *          A topic representing either a topic type or an association type.
     */
    TopicModel fetchParentType(Association assoc);
}
