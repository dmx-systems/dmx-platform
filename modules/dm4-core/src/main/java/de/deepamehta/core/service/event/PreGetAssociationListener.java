package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface PreGetAssociationListener extends EventListener {

    void preGetAssociation(long assocId);
}
