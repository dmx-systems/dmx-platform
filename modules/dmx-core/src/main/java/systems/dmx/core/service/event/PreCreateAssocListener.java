package systems.dmx.core.service.event;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.service.EventListener;



public interface PreCreateAssocListener extends EventListener {

    void preCreateAssociation(AssocModel model);
}
