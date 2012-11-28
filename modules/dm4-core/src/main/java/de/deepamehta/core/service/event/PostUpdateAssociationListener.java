package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PostUpdateAssociationListener extends Listener {

    void postUpdateAssociation(Association assoc, AssociationModel oldModel, ClientState clientState,
                                                                             Directives directives);
}
