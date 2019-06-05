package systems.dmx.core.service.event;

import systems.dmx.core.Assoc;
import systems.dmx.core.service.EventListener;



public interface PreDeleteAssociationListener extends EventListener {

    void preDeleteAssociation(Assoc assoc);
}
