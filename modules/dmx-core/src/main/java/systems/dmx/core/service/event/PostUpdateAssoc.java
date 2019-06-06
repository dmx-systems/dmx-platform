package systems.dmx.core.service.event;

import systems.dmx.core.Assoc;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.service.EventListener;



public interface PostUpdateAssoc extends EventListener {

    void postUpdateAssoc(Assoc assoc, AssocModel updateModel, AssocModel oldAssoc);
}
