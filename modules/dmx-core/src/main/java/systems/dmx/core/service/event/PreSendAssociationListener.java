package systems.dmx.core.service.event;

import systems.dmx.core.Association;
import systems.dmx.core.service.EventListener;



public interface PreSendAssociationListener extends EventListener {

    void preSendAssociation(Association assoc);
}
