package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface CheckAssociationWriteAccessListener extends EventListener {

    void checkAssociationWriteAccess(long assocId);
}
