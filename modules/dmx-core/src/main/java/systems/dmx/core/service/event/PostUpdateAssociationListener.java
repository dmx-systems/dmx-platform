package systems.dmx.core.service.event;

import systems.dmx.core.Association;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.service.EventListener;



public interface PostUpdateAssociationListener extends EventListener {

    void postUpdateAssociation(Association assoc, AssociationModel updateModel, AssociationModel oldAssoc);
}
