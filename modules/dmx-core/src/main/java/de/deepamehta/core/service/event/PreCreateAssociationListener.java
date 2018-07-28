package de.deepamehta.core.service.event;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.service.EventListener;



public interface PreCreateAssociationListener extends EventListener {

    void preCreateAssociation(AssociationModel model);
}
