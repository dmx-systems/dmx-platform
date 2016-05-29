package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface CheckAssociationReadAccessListener extends EventListener {

    void checkAssociationReadAccess(long assocId);
}
