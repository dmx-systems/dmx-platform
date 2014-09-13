package de.deepamehta.core;

import de.deepamehta.core.model.AssociationTypeModel;



public interface AssociationType extends Type {

    // === Updating ===

    void update(AssociationTypeModel model);
}
