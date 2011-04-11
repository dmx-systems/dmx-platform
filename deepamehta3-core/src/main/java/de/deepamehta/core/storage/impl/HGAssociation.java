package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationRole;
import de.deepamehta.core.model.TopicRole;

import java.util.Set;



class HGAssociation extends AssociationData implements Association {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGAssociation(long id, String typeUri, Set<TopicRole> topicRoles, Set<AssociationRole> assocRoles) {
        super(id, typeUri, topicRoles, assocRoles);
    }
}
