package de.deepamehta.core.impl.storage;

import de.deepamehta.core.impl.model.AssociationBase;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RoleModel;



class HGAssociation extends AssociationBase {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGAssociation(long id, String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        super(new AssociationModel(id, typeUri, roleModel1, roleModel2));
    }
}
