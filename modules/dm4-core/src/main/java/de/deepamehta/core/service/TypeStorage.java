package de.deepamehta.core.service;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;



public interface TypeStorage {

    /**
     * @param   assoc   an association representing an association definition
     *
     * @return  the parent type topic.
     *          A topic representing either a topic type or an association type.
     */
    TopicModel fetchParentType(AssociationModel assoc);
}
