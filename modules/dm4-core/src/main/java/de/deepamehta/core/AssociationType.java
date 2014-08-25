package de.deepamehta.core;

import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.service.Directives;



public interface AssociationType extends Type {

    // === Updating ===

    void update(AssociationTypeModel model, Directives directives);
}
