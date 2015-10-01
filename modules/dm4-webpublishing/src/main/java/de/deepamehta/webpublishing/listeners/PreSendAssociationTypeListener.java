package de.deepamehta.webpublishing.listeners;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.service.EventListener;



public interface PreSendAssociationTypeListener extends EventListener {

    void preSendAssociationType(AssociationType assocType);
}
