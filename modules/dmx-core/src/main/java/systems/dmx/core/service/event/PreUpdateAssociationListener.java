package systems.dmx.core.service.event;

import systems.dmx.core.Assoc;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.service.EventListener;



public interface PreUpdateAssociationListener extends EventListener {

    void preUpdateAssociation(Assoc assoc, AssociationModel updateModel);
}
