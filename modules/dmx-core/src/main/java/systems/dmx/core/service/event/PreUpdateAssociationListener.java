package systems.dmx.core.service.event;

import systems.dmx.core.Association;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.service.EventListener;



public interface PreUpdateAssociationListener extends EventListener {

    void preUpdateAssociation(Association assoc, AssociationModel updateModel);
}
