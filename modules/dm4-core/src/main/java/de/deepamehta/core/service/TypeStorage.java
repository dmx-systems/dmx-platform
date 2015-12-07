package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.AssociationDefinitionModel;



public interface TypeStorage {

    AssociationDefinitionModel createAssociationDefinition(Association assoc);

    AssociationDefinitionModel fetchAssociationDefinition(Association assoc);

    // ---

    /**
     * @param   assoc   an association representing an association definition
     *
     * @return  the parent type topic.
     *          A topic representing either a topic type or an association type.
     */
    TopicModel fetchParentType(AssociationModel assoc);

    /**
     * @param   assoc   an association representing an association definition
     *
     * @return  the child type topic.
     *          A topic representing a topic type.
     */
    TopicModel fetchChildType(AssociationModel assoc);

    // ---

    // Removes an association from memory and rebuilds the sequence in DB. Note: the underlying
    // association is *not* removed from DB.
    // This method is called (by the Type Editor plugin's preDeleteAssociation() hook) when the
    // deletion of an association that represents an association definition is imminent.
    void removeAssociationDefinitionFromMemoryAndRebuildSequence(Type type, String childTypeUri);
}
