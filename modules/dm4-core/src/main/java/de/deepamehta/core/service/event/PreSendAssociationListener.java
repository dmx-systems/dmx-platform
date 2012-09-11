package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface PreSendAssociationListener extends Listener {

    void preSendAssociation(Association assoc, ClientState clientState);
}
