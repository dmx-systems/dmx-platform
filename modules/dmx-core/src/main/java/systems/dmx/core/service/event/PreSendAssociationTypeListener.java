package systems.dmx.core.service.event;

import systems.dmx.core.AssociationType;
import systems.dmx.core.service.EventListener;



public interface PreSendAssociationTypeListener extends EventListener {

    void preSendAssociationType(AssociationType assocType);
}
