package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.service.EventListener;



public interface PreUpdateAssociationListener extends EventListener {

    void preUpdateAssociation(Association assoc, AssociationModel updateModel);
}
