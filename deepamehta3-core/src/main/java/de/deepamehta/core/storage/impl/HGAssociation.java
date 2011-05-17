package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.impl.AssociationBase;



class HGAssociation extends AssociationBase {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGAssociation(long id, String typeUri, Role role1, Role role2) {
        super(new AssociationModel(id, typeUri, role1, role2));
    }
}
