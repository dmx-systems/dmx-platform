package de.deepamehta.core.service.listener;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface PreCreateAssociationListener extends Listener {

    void preCreateAssociation(AssociationModel model, ClientState clientState);
}
