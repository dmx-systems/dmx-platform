package systems.dmx.accesscontrol.event;

import systems.dmx.core.service.EventListener;



public interface PostLogoutUserListener extends EventListener {

    void postLogoutUser(String username);
}
