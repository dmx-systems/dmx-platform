package systems.dmx.accesscontrol.event;

import systems.dmx.core.service.EventListener;



public interface PostLoginUserListener extends EventListener {

    void postLoginUser(String username);
}
