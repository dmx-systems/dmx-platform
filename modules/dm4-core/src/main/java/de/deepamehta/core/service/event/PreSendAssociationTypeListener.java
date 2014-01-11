package de.deepamehta.core.service.event;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.EventListener;



public interface PreSendAssociationTypeListener extends EventListener {

    void preSendAssociationType(AssociationType assocType, ClientState clientState);
}
