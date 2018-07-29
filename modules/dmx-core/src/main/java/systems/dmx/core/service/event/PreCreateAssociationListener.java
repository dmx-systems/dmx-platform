package systems.dmx.core.service.event;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.service.EventListener;



public interface PreCreateAssociationListener extends EventListener {

    void preCreateAssociation(AssociationModel model);
}
