package de.deepamehta.core.impl.storage;

import de.deepamehta.core.impl.model.AssociationBase;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.Role;



class HGAssociation extends AssociationBase {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGAssociation(long id, String typeUri, Role role1, Role role2) {
        super(new AssociationModel(id, typeUri, role1, role2));
    }
}
