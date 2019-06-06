package systems.dmx.core.service.event;

import systems.dmx.core.AssocType;
import systems.dmx.core.service.EventListener;



public interface PreSendAssociationTypeListener extends EventListener {

    void preSendAssociationType(AssocType assocType);
}
