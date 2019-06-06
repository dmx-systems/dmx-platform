package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface CheckAssocReadAccess extends EventListener {

    void checkAssocReadAccess(long assocId);
}
