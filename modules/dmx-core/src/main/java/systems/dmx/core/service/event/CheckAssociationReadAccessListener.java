package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckAssociationReadAccessListener extends EventListener {

    void checkAssociationReadAccess(long assocId);
}
