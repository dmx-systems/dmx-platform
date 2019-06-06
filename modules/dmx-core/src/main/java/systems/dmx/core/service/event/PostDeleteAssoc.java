package systems.dmx.core.service.event;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.service.EventListener;



public interface PostDeleteAssoc extends EventListener {

    void postDeleteAssoc(AssocModel assoc);
}
