package systems.dmx.core.service.event;

import systems.dmx.core.Assoc;
import systems.dmx.core.service.EventListener;



public interface PostCreateAssoc extends EventListener {

    void postCreateAssoc(Assoc assoc);
}
