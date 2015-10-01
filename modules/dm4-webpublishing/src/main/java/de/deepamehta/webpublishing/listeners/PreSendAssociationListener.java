package de.deepamehta.webpublishing.listeners;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.EventListener;



public interface PreSendAssociationListener extends EventListener {

    void preSendAssociation(Association assoc);
}
