package de.deepamehta.core.service.event;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface PreSendAssociationTypeListener extends Listener {

    void preSendAssociationType(AssociationType assocType, ClientState clientState);
}
