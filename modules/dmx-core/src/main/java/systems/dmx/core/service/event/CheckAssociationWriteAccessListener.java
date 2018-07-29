package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckAssociationWriteAccessListener extends EventListener {

    void checkAssociationWriteAccess(long assocId);
}
