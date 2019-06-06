package systems.dmx.core.service.event;

import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.service.EventListener;



public interface PreCreateAssocType extends EventListener {

    void preCreateAssocType(AssocTypeModel model);
}
