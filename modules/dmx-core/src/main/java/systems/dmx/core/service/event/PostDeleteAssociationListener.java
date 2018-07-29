package systems.dmx.core.service.event;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.service.EventListener;



public interface PostDeleteAssociationListener extends EventListener {

    void postDeleteAssociation(AssociationModel assoc);
}
