package systems.dmx.core.service.event;

import systems.dmx.core.AssocType;
import systems.dmx.core.service.EventListener;



public interface PreSendAssocTypeListener extends EventListener {

    void preSendAssocType(AssocType assocType);
}
