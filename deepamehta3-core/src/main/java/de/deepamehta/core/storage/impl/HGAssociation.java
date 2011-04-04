package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.Role;

import java.util.Set;



class HGAssociation extends AssociationData implements Association {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGAssociation(long id, String typeUri, Set<Role> roles) {
        super(id, typeUri, roles);
    }
}
